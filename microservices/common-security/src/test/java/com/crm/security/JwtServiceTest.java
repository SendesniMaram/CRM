package com.crm.security;

import java.util.List;
import java.util.Date;
import java.nio.charset.StandardCharsets;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-for-jwt-service-at-least-32-bytes";
    private static final String OTHER_TEST_SECRET = "other-test-secret-for-jwt-service-at-least-32-bytes";

    private final JwtService jwtService = new JwtService(TEST_SECRET);

    @Test
    void generateToken_shouldContainUsernameEmailAndRoles() {
        String token = jwtService.generateToken("john", "john@example.com", true,
                List.of("ROLE_ADMIN", "ROLE_EMPLOYEE"));

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("john", jwtService.extractUsername(token));
        assertEquals(List.of("ROLE_ADMIN", "ROLE_EMPLOYEE"), jwtService.extractRoles(token));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void generateToken_withoutRoles_shouldReturnEmptyRoles() {
        String token = jwtService.generateToken("alice", "alice@example.com", true, null);

        assertTrue(jwtService.isTokenValid(token));
        assertTrue(jwtService.extractRoles(token).isEmpty());
    }

    @Test
    void invalidToken_shouldBeRejected() {
        assertFalse(jwtService.isTokenValid("invalid.token.here"));
        assertNull(jwtService.extractUsername("invalid.token.here"));
        assertTrue(jwtService.extractRoles("invalid.token.here").isEmpty());
        assertTrue(jwtService.isTokenExpired("invalid.token.here"));
    }

    @Test
    void tokenSignedWithDifferentSecret_shouldBeRejected() {
        JwtService other = new JwtService(OTHER_TEST_SECRET);
        String token = other.generateToken("bob", "bob@example.com", true, List.of("ROLE_CLIENT"));

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void expiredToken_shouldBeRejected() {
        String expired = Jwts.builder().subject("john").claim("enabled", true)
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertTrue(jwtService.isTokenExpired(expired));
        assertFalse(jwtService.isTokenValid(expired));
    }

    @Test
    void disabledOrMissingEnabledClaimCannotAuthorize() {
        assertFalse(jwtService.isTokenValid(jwtService.generateToken("john", "", false, List.of("ADMIN"))));
        String missingClaim = Jwts.builder().subject("john")
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertFalse(jwtService.isTokenValid(missingClaim));
    }

    @Test
    void blankSubjectCannotAuthorize() {
        assertFalse(jwtService.isTokenValid(jwtService.generateToken(" ", "", true, List.of("ADMIN"))));
    }
}

