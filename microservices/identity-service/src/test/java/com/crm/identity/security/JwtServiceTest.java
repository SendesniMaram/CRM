package com.crm.identity.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    private static final String KEY = "synthetic-identity-jwt-key-at-least-32-bytes";
    private final JwtService jwt = new JwtService(KEY);

    @Test
    void currentTokenFormatPreservesRolesAndExpiration() {
        String token = jwt.generateToken("user", "user@example.test", true, List.of("ADMIN", "EMPLOYEE"));
        assertThat(jwt.isTokenValid(token, "user")).isTrue();
        assertThat(jwt.extractRoles(token)).containsExactly("ADMIN", "EMPLOYEE");
        assertThat(jwt.isTokenValid(token, "someone-else")).isFalse();
    }

    @Test
    void disabledClaimCannotAuthorizeEvenWithValidSignature() {
        assertThat(jwt.isTokenValid(jwt.generateToken("user", "", false, List.of("ADMIN")), "user")).isFalse();
    }

    @Test
    void missingEnabledClaimOrExpirationCannotAuthorize() {
        var key = Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8));
        String missingEnabled = Jwts.builder().subject("user")
                .expiration(new Date(System.currentTimeMillis() + 60000)).signWith(key).compact();
        String missingExpiration = Jwts.builder().subject("user").claim("enabled", true).signWith(key).compact();
        assertThat(jwt.isTokenValid(missingEnabled, "user")).isFalse();
        assertThat(jwt.isTokenValid(missingExpiration, "user")).isFalse();
    }

    @Test
    void expiredInvalidAndForeignTokensCannotAuthorize() {
        String expired = Jwts.builder().subject("user").claim("enabled", true)
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8))).compact();
        String foreign = new JwtService("different-synthetic-identity-key-at-least-32-bytes")
                .generateToken("user", "", true, List.of("ADMIN"));
        assertThat(jwt.isTokenValid(expired, "user")).isFalse();
        assertThat(jwt.isTokenValid("malformed-token", "user")).isFalse();
        assertThat(jwt.isTokenValid(foreign, "user")).isFalse();
        assertThat(jwt.isTokenValid(jwt.generateToken(" ", "", true, List.of()), " ")).isFalse();
    }
}
