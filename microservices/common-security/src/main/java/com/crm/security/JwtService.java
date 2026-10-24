package com.crm.security;

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
 * Shared JWT service used by all CRM microservices.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Generate tokens (used only by identity-service)</li>
 *   <li>Validate signature</li>
 *   <li>Validate expiration</li>
 *   <li>Extract username</li>
 *   <li>Extract roles</li>
 * </ul>
 * Validation is done locally from the token only — no database access.
 */
public class JwtService {

    /** Strict consumer contract, aligned with Identity's internal validator. */
    public AccessTokenClaims parseAccessToken(String token) {
        Claims claims = extractAllClaims(token);
        Object uid = claims.get("uid");
        if (!(uid instanceof String value) || !value.matches("[1-9][0-9]{0,18}")
                || claims.getSubject() == null || claims.getSubject().isBlank()
                || !Boolean.TRUE.equals(claims.get(CLAIM_ENABLED))
                || claims.getExpiration() == null || !claims.getExpiration().after(new Date())) {
            throw new IllegalArgumentException("Invalid access token");
        }
        long userId = Long.parseLong(value);
        if (!(claims.get(CLAIM_ROLES) instanceof List<?> values) || values.isEmpty()) {
            throw new IllegalArgumentException("Invalid access token");
        }
        List<String> roles = values.stream().map(role -> {
            if (!(role instanceof String name)) throw new IllegalArgumentException("Invalid access token");
            String normalized = name.startsWith("ROLE_") ? name.substring(5) : name;
            if (!java.util.Set.of("ADMIN", "SUPER_ADMIN", "EMPLOYEE", "CLIENT").contains(normalized)) {
                throw new IllegalArgumentException("Invalid access token");
            }
            return normalized;
        }).distinct().toList();
        return new AccessTokenClaims(userId, claims.getSubject(), roles, claims.getExpiration().toInstant());
    }

    public record AccessTokenClaims(long userId, String username, List<String> roles,
                                    java.time.Instant expiresAt) {}

    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ENABLED = "enabled";

    private static final long ONE_HOUR_MS = 1000L * 60 * 60;
    private final SecretKey secretKey;

    public JwtService(String secret) {
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret must contain at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a JWT containing username (subject), email, enabled flag and roles.
     *
     * @param username subject of the token
     * @param email    email claim
     * @param enabled  enabled claim
     * @param roles    roles claim (e.g. ["ROLE_ADMIN", "ROLE_EMPLOYEE"])
     */
    public String generateToken(String username, String email, boolean enabled, Collection<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ONE_HOUR_MS);

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

    /**
     * Parse and verify the token signature. Throws {@link JwtException} on invalid
     * signature, malformed token or expired token.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract the username (subject) from the token. Returns {@code null} if the
     * token is invalid, expired or malformed.
     */
    public String extractUsername(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Extract the roles claim from the token. Returns an empty list if the token
     * is invalid or the claim is absent.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object roles = claims.get(CLAIM_ROLES);
            if (roles instanceof List<?> list) {
                return list.stream().map(String::valueOf).toList();
            }
            return List.of();
        } catch (JwtException | IllegalArgumentException e) {
            return List.of();
        }
    }

    /**
     * Legacy local inspection helper, insufficient for authenticating a request.
     * Authentication uses parseAccessToken and AccountValidationClient instead.
     * The enabled claim is an issuance snapshot, not a live account-status lookup.
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date())
                    && Boolean.TRUE.equals(claims.get(CLAIM_ENABLED))
                    && claims.getSubject() != null && !claims.getSubject().isBlank();
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * True if the token is expired or cannot be parsed.
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration() == null || !claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }
}

