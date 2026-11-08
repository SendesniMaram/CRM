package com.crm.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;

/** All seven consumers keep the same shared filter; Identity remains the final session authority. */
class SessionBoundTokenCompatibilityTest {
    @ParameterizedTest @ValueSource(strings={"SUPER_ADMIN","ADMIN","EMPLOYEE","CLIENT"})
    void extendedTokenWorksUntilIdentityRejectsTheSessionAndIsNeverCached(String role) throws Exception {
        String key = "synthetic-shared-session-contract-secret-32-bytes";
        String token = Jwts.builder().subject("worker").claim("uid","42").claim("enabled",true)
                .claim("sid","11111111-1111-4111-8111-111111111111").claim("roles",List.of(role))
                .expiration(new Date(System.currentTimeMillis()+60000))
                .signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8))).compact();
        var status = new AtomicInteger(204);
        try (var client = new AccountValidationClient(new JwtService(key), AccountValidationClientTest.properties(),
                header -> status.get(), Clock.systemUTC())) {
            var filter = new JwtAuthenticationFilter(client);
            for (int remote : new int[]{204,401,503}) {
                status.set(remote);
                var request = new MockHttpServletRequest("GET","/api/business");
                request.addHeader("Authorization","Bearer "+token);
                var response = new MockHttpServletResponse(); boolean[] reached = {false};
                filter.doFilter(request,response,(r,s)->reached[0]=true);
                if (remote==204) {
                    assertTrue(reached[0]);
                    var principal = AuthenticatedUserPrincipal.require(SecurityContextHolder.getContext().getAuthentication());
                    assertEquals(42,principal.userId()); assertEquals("worker",principal.username());
                    assertEquals("ROLE_"+role,principal.authorities().get(0).getAuthority());
                } else { assertFalse(reached[0]); assertEquals(remote,response.getStatus()); assertNull(SecurityContextHolder.getContext().getAuthentication()); }
            }
        } finally { SecurityContextHolder.clearContext(); }
    }
}
