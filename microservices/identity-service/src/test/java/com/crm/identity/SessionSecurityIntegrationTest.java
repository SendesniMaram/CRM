package com.crm.identity;

import com.crm.identity.dto.RefreshTokenRequest;
import com.crm.identity.entity.RefreshToken;
import com.crm.identity.entity.User;
import com.crm.identity.enums.RoleType;
import com.crm.identity.exception.InvalidCredentialsException;
import com.crm.identity.repository.RefreshTokenRepository;
import com.crm.identity.repository.RoleRepository;
import com.crm.identity.repository.UserRepository;
import com.crm.identity.security.JwtService;
import com.crm.identity.service.IAuthenticationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.name=application-test", "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:sprint1_sessions;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "eureka.client.enabled=false", "spring.cloud.service-registry.auto-registration.enabled=false",
        "app.bootstrap.admin.enabled=false"
})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class SessionSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired RefreshTokenRepository tokens;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder encoder;
    @Autowired IAuthenticationService authentication;

    // Deliberately no test transaction: assert persisted revocation after the HTTP 401.
    @BeforeEach
    void clean() { tokens.deleteAll(); users.deleteAll(); }

    private User user(RoleType role) {
        User user = new User();
        user.setUsername("session-test");
        user.setEmail("session@example.test");
        user.setPassword(encoder.encode("synthetic-password"));
        user.getRole().add(roles.findByRoleType(role).orElseThrow());
        return users.saveAndFlush(user);
    }

    private RefreshToken token(User user, String value) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(value);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        return tokens.saveAndFlush(token);
    }

    private String access(User user) {
        return jwt.generateToken(user.getUsername(), user.getEmail(), true,
                user.getRole().stream().map(r -> r.getRoleType().name()).toList());
    }

    private void refresh(String value, int expected) throws Exception {
        mvc.perform(post("/api/auth/refresh").contentType("application/json")
                .content("{\"refreshToken\":\"" + value + "\"}"))
                .andExpect(status().is(expected));
    }

    @ParameterizedTest
    @EnumSource(RoleType.class)
    void activeAccountsCanLoginWithoutChangingRolePermissions(RoleType role) throws Exception {
        User user = user(role);
        var response = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"session-test\",\"password\":\"synthetic-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value(role.name()))
                .andExpect(jsonPath("$.token").isString()).andExpect(jsonPath("$.refreshToken").isString())
                .andReturn().getResponse();
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getContentAsString());
        String access = json.get("token").asText();
        assertThat(jwt.isTokenValid(access, user.getUsername())).isTrue();
        assertThat(tokens.count()).isEqualTo(1);
        int expected = role == RoleType.ADMIN || role == RoleType.SUPER_ADMIN ? 200 : 403;
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + access))
                .andExpect(status().is(expected));
    }

    @ParameterizedTest
    @EnumSource(RoleType.class)
    void activeAccountsOfEveryRoleCanRefreshAndOldTokenCannotBeReplayed(RoleType role) throws Exception {
        User user = user(role);
        token(user, "valid-refresh");
        mvc.perform(post("/api/auth/refresh").contentType("application/json")
                .content("{\"refreshToken\":\"valid-refresh\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.roles[0]").value(role.name()))
                .andExpect(jsonPath("$.type").value("Bearer"));
        assertThat(tokens.findByToken("valid-refresh").orElseThrow().isRevoked()).isTrue();
        assertThat(tokens.count()).isEqualTo(2);
        refresh("valid-refresh", 401);
        assertThat(tokens.count()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void statusChangeBlocksOldAccessInIdentityAndDurablyRevokesAllRefreshTokens(boolean disable) throws Exception {
        User user = user(RoleType.ADMIN);
        String access = access(user);
        token(user, "session-one"); token(user, "session-two");
        User other = new User();
        other.setUsername("other"); other.setEmail("other@example.test"); other.setPassword("unused-synthetic");
        other = users.saveAndFlush(other);
        token(other, "unrelated-session");
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + access)).andExpect(status().isOk());
        if (disable) user.setEnabled(false); else user.setAccountNonLocked(false);
        users.saveAndFlush(user);
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + access)).andExpect(status().isUnauthorized());
        refresh("session-one", 401);
        assertThat(tokens.findByToken("session-one").orElseThrow().isRevoked()).isTrue();
        assertThat(tokens.findByToken("session-two").orElseThrow().isRevoked()).isTrue();
        assertThat(tokens.findByToken("unrelated-session").orElseThrow().isRevoked()).isFalse();
        assertThat(tokens.count()).isEqualTo(3);
        user.setEnabled(true); user.setAccountNonLocked(true); users.saveAndFlush(user);
        refresh("session-two", 401);
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + access)).andExpect(status().isOk());
        // Existing access tokens are not permanently revoked by a status toggle.
    }

    @Test
    void expiredTokenRevocationSurvives401TransactionBoundary() throws Exception {
        User user = user(RoleType.EMPLOYEE);
        RefreshToken expired = token(user, "expired-refresh");
        expired.setExpiryDate(LocalDateTime.now().minusSeconds(1)); tokens.saveAndFlush(expired);
        refresh("expired-refresh", 401);
        assertThat(tokens.findByToken("expired-refresh").orElseThrow().isRevoked()).isTrue();
        assertThat(tokens.count()).isEqualTo(1);
    }

    @Test
    void unknownAndRevokedTokensCannotIssueSessions() throws Exception {
        User user = user(RoleType.CLIENT);
        RefreshToken revoked = token(user, "revoked-refresh");
        revoked.setRevoked(true); tokens.saveAndFlush(revoked);
        refresh("unknown-refresh", 401); refresh("revoked-refresh", 401);
        assertThat(tokens.count()).isEqualTo(1);
    }

    @Test
    void deletedAccountCannotUsePreviouslyIssuedIdentityAccessToken() throws Exception {
        User user = user(RoleType.ADMIN);
        String access = access(user);
        users.deleteById(user.getId());
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletedJwtSubjectCannotResolveAnotherAccountThroughItsEmail() throws Exception {
        User deleted = user(RoleType.EMPLOYEE);
        deleted.setUsername("shared@example.test"); users.saveAndFlush(deleted);
        String oldAccess = access(deleted);
        users.deleteById(deleted.getId());
        User unrelatedAdmin = user(RoleType.ADMIN);
        unrelatedAdmin.setEmail("shared@example.test"); users.saveAndFlush(unrelatedAdmin);
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + oldAccess))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emailLoginStillResolvesTheActualUsername() throws Exception {
        user(RoleType.EMPLOYEE);
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"email\":\"session@example.test\",\"password\":\"synthetic-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("session-test"))
                .andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void loginRemainsForbiddenForInactiveAccounts(boolean disable) throws Exception {
        User user = user(RoleType.ADMIN);
        if (disable) user.setEnabled(false); else user.setAccountNonLocked(false);
        users.saveAndFlush(user);
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"session-test\",\"password\":\"synthetic-password\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(tokens.count()).isZero();
    }

    @Test
    void concurrentRefreshHasOnlyOneSuccessfulConsumer() throws Exception {
        User user = user(RoleType.EMPLOYEE);
        token(user, "concurrent-refresh");
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> consume = () -> {
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Test start timed out");
                RefreshTokenRequest request = new RefreshTokenRequest();
                request.setRefreshToken("concurrent-refresh");
                try { authentication.refreshToken(request); return true; }
                catch (InvalidCredentialsException ex) { return false; }
            };
            var first = executor.submit(consume); var second = executor.submit(consume); start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(tokens.count()).isEqualTo(2);
        assertThat(tokens.findAll().stream().filter(t -> !t.isRevoked()).count()).isEqualTo(1);
    }
}
