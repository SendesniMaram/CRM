package com.crm.security;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.*;

class JwtAuthenticationFilterTest {
    private final JwtService jwt = AccountValidationClientTest.JWT;
    private final AccountValidationClient client = new AccountValidationClient(jwt, AccountValidationClientTest.properties(),
            header -> 204, java.time.Clock.systemUTC());
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(client);

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); client.close(); }

    private void authenticate(String token) throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
    }

    @Test
    void disabledClaimCannotBuildAuthentication() throws Exception {
        authenticate(jwt.generateToken("employee", "", false, List.of("ADMIN")));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validTokenPreservesSubjectAndRoleAuthorities() throws Exception {
        authenticate(AccountValidationClientTest.bearer().substring(7));
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = AuthenticatedUserPrincipal.require(authentication);
        assertThat(principal.userId()).isEqualTo(42L);
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(principal.authorities()).isEqualTo(List.copyOf(authentication.getAuthorities()));
        assertThat(authentication.getName()).isEqualTo("alice");
        assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_EMPLOYEE", "ROLE_ADMIN");
    }

    @Test
    void remoteInvalidationOverridesSignedIssuanceSnapshot() throws Exception {
        String beforeStatusChange = AccountValidationClientTest.bearer().substring(7);
        authenticate(beforeStatusChange);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(jwt.isTokenValid(beforeStatusChange)).isTrue();
        try (var rejectingClient = new AccountValidationClient(jwt, AccountValidationClientTest.properties(), h -> 401, java.time.Clock.systemUTC())) {
            var request = new MockHttpServletRequest("GET", "/api/employees");
            request.addHeader("Authorization", "Bearer " + beforeStatusChange);
            var response = new MockHttpServletResponse();
            new JwtAuthenticationFilter(rejectingClient).doFilter(request, response, (req, res) -> fail("handler executed"));
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Test
    void malformedTokenDoesNotAuthenticate() throws Exception {
        authenticate("invalid-token");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {401, 200, 403, 404, 429, 500, 503})
    void remoteFailureNeverExecutesHandlerOrLeaksBearer(int status) throws Exception {
        try (var remote = new AccountValidationClient(jwt, AccountValidationClientTest.properties(), h -> status, java.time.Clock.systemUTC())) {
            var request = new MockHttpServletRequest("GET", "/api/payrolls/1/pdf");
            request.addHeader("Authorization", AccountValidationClientTest.bearer());
            var response = new MockHttpServletResponse();
            new JwtAuthenticationFilter(remote).doFilter(request, response, (req, res) -> fail("handler executed"));
            assertThat(response.getStatus()).isEqualTo(status == 401 ? 401 : 503);
            assertThat(response.getContentType()).startsWith("application/json");
            assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
            assertThat(response.getContentAsString()).doesNotContain("Bearer", "eyJ");
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Test void missingBearerBlocksHandler() throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/employees"), response, (req, res) -> fail("handler executed"));
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test void clientIdentityHeadersCannotOverrideValidatedPrincipal() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/leaves");
        request.addHeader("Authorization", AccountValidationClientTest.bearer());
        request.addHeader("X-User-Id", "999");
        request.addHeader("X-Username", "other-user");
        request.addHeader("X-Roles", "SUPER_ADMIN");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(AuthenticatedUserPrincipal.require(auth).userId()).isEqualTo(42L);
            assertThat(auth.getName()).isEqualTo("alice");
            assertThat(auth.getAuthorities()).extracting("authority")
                    .containsExactly("ROLE_EMPLOYEE", "ROLE_ADMIN");
        });
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"", "0", "-1", "01", "1.0", " 42", "abc", "9223372036854775808"})
    void invalidUidNeverCreatesPrincipalOrCallsIdentity(String uid) throws Exception {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        try (var remote = new AccountValidationClient(jwt, AccountValidationClientTest.properties(), h -> {
            calls.incrementAndGet();
            return 204;
        }, java.time.Clock.systemUTC())) {
            var request = new MockHttpServletRequest("GET", "/api/leaves");
            request.addHeader("Authorization", "Bearer " + AccountValidationClientTest.token(uid,
                    List.of("EMPLOYEE"), true, "alice", java.time.Instant.now().plusSeconds(60)));
            var response = new MockHttpServletResponse();
            new JwtAuthenticationFilter(remote).doFilter(request, response, (req, res) -> fail("handler executed"));
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(calls).hasValue(0);
        }
    }

    @Test void numericUidIsNotAcceptedAsCanonicalStringIdentity() throws Exception {
        authenticate(AccountValidationClientTest.token(42L, List.of("EMPLOYEE"), true,
                "alice", java.time.Instant.now().plusSeconds(60)));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"/actuator/health", "/api/test", "/v3/api-docs", "/v3/api-docs/config", "/swagger-ui/index.html"})
    void exactPublicResourcesRemainAvailable(String path) throws Exception {
        var response = new MockHttpServletResponse();
        var called = new java.util.concurrent.atomic.AtomicBoolean();
        filter.doFilter(new MockHttpServletRequest("GET", path), response, (req, res) -> called.set(true));
        assertThat(called).isTrue();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"/actuator/health/private", "/api/test/private", "/swagger-ui-evil", "/api/employees"})
    void publicPrefixesDoNotExemptBusinessRoutes(String path) throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", path), response, (req, res) -> fail("handler executed"));
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test void successfulAuthenticationLeavesBusiness403Untouched() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/invoices");
        request.addHeader("Authorization", AccountValidationClientTest.bearer());
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(403));
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test void publicPostIsNotExempted() throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("POST", "/api/test"), response, (req, res) -> fail("handler executed"));
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"ADMIN", "SUPER_ADMIN"})
    void springAuthorizationUsesOnlySignedRoles(String requiredRole) throws Exception {
        var exceptionFilter = new org.springframework.security.web.access.ExceptionTranslationFilter(new RestAuthenticationEntryPoint());
        exceptionFilter.setAccessDeniedHandler(new CustomAccessDeniedHandler());
        var authorizationFilter = new org.springframework.security.web.access.intercept.AuthorizationFilter(
                org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole(requiredRole));
        var security = new org.springframework.security.web.FilterChainProxy(
                new org.springframework.security.web.DefaultSecurityFilterChain(
                        org.springframework.security.web.util.matcher.AnyRequestMatcher.INSTANCE,
                        filter, exceptionFilter, authorizationFilter));
        var request = new MockHttpServletRequest("GET", "/api/employees");
        request.setServletPath("/api/employees");
        request.addHeader("Authorization", AccountValidationClientTest.bearer());
        var response = new MockHttpServletResponse();
        var called = new java.util.concurrent.atomic.AtomicBoolean();
        security.doFilter(request, response, (req, res) -> called.set(true));
        assertThat(response.getStatus()).isEqualTo(requiredRole.equals("ADMIN") ? 200 : 403);
        assertThat(called.get()).isEqualTo(requiredRole.equals("ADMIN"));
    }

    @Test void networkFailureCannotExecuteBusinessHandler() throws Exception {
        try (var unavailable = new AccountValidationClient(jwt, AccountValidationClientTest.properties(),
                h -> { throw new java.net.UnknownHostException("synthetic.invalid"); }, java.time.Clock.systemUTC())) {
            var request = new MockHttpServletRequest("GET", "/api/employees");
            request.addHeader("Authorization", AccountValidationClientTest.bearer());
            var response = new MockHttpServletResponse();
            new JwtAuthenticationFilter(unavailable).doFilter(request, response, (req, res) -> fail("handler executed"));
            assertThat(response.getStatus()).isEqualTo(503);
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"malformed", "legacy", "uid", "expired"})
    void localRejectionCannotExecuteBusinessHandler(String kind) throws Exception {
        String token = switch (kind) {
            case "malformed" -> "invalid-token";
            case "legacy" -> jwt.generateToken("alice", "", true, List.of("CLIENT"));
            case "uid" -> AccountValidationClientTest.token("01", List.of("CLIENT"), true, "alice", java.time.Instant.now().plusSeconds(60));
            default -> AccountValidationClientTest.token("42", List.of("CLIENT"), true, "alice", java.time.Instant.now().minusSeconds(5));
        };
        var request = new MockHttpServletRequest("GET", "/api/employees");
        request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("handler executed"));
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
