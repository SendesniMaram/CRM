package com.crm.identity.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT service for identity-service.
 * <p>
 * Generates tokens with a {@code roles} claim so that downstream services
 * (using common-security) can build authorities from the token without
 * accessing the database.
 */
public class JwtService {

    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ENABLED = "enabled";

    private static final String SECRET_ENV_VAR = "APP_JWT_SECRET";
    private static final String DEFAULT_SECRET = "0123456789abcdef0123456789abcdef";

    private final SecretKey secretKey;

    public JwtService() {
        this(resolveSecretFromEnvironment());
    }

    private static String resolveSecretFromEnvironment() {
        String secret = System.getenv(SECRET_ENV_VAR);
        if (secret == null || secret.isBlank()) {
            secret = System.getProperty("app.jwt.secret");
        }
        if (secret == null || secret.isBlank()) {
            secret = System.getProperty(SECRET_ENV_VAR);
        }
        if (secret == null || secret.isBlank()) {
            return DEFAULT_SECRET;
        }
        return secret;
    }

    public JwtService(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String email, boolean enabled) {
        return generateToken(username, email, enabled, List.of());
    }

    public String generateToken(String username, String email, boolean enabled, Collection<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 1000L * 60 * 60); // 1 hour

        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ENABLED, enabled)
                .claim(CLAIM_ROLES, roles == null ? List.of() : new ArrayList<>(roles))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Extract the roles claim from the token. Returns an empty list if the claim
     * is absent or the token cannot be parsed.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object roles = claims.get(CLAIM_ROLES);
            if (roles instanceof List<?> list) {
                return list.stream().map(String::valueOf).toList();
            }
            return List.of();
        } catch (JwtException | IllegalArgumentException e) {
            return List.of();
        }
    }

    public boolean isTokenValid(String token, String username) {
        String extracted = extractUsername(token);
        return extracted != null && extracted.equals(username);
    }
}

