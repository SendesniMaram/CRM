package com.crm.identity.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.time.Instant;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.crm.identity.enums.RoleType;
import com.crm.identity.exception.InvalidCredentialsException;

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
    public static final String CLAIM_USER_ID = "uid";

    public static final long ACCESS_TOKEN_DURATION_MS = 1000L * 60 * 60;

    private final SecretKey secretKey;

    public JwtService(String secret) {
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret must contain at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String email, boolean enabled) {
        return generateToken(username, email, enabled, List.of());
    }

    public String generateToken(String username, String email, boolean enabled, Collection<String> roles) {
        return buildToken(null, username, email, enabled, roles);
    }

    /** Production issuance uses the authenticated, persisted User id. */
    public String generateToken(Long userId, String username, String email, boolean enabled, Collection<String> roles) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Authenticated user id must be positive");
        }
        return buildToken(userId, username, email, enabled, roles);
    }

    private String buildToken(Long userId, String username, String email, boolean enabled, Collection<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_DURATION_MS);

        var builder = Jwts.builder()
                .subject(username)
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ENABLED, enabled)
                .claim(CLAIM_ROLES, roles == null ? List.of() : new ArrayList<>(roles))
                .issuedAt(now)
                .expiration(expiry);
        if (userId != null) builder.claim(CLAIM_USER_ID, userId.toString());
        return builder.signWith(secretKey).compact();
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
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload();
            return username != null && !username.isBlank() && username.equals(claims.getSubject())
                    && Boolean.TRUE.equals(claims.get(CLAIM_ENABLED))
                    && claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Strict validation for the internal access-check contract. */
    public AccessTokenClaims parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload();
            Object uid = claims.get(CLAIM_USER_ID);
            if (!(uid instanceof String value) || !value.matches("[1-9][0-9]{0,18}")
                    || claims.getSubject() == null || claims.getSubject().isBlank()
                    || !Boolean.TRUE.equals(claims.get(CLAIM_ENABLED))
                    || claims.getExpiration() == null || !claims.getExpiration().after(new Date())) {
                throw new InvalidCredentialsException("Invalid access token");
            }
            long userId = Long.parseLong(value);
            if (!(claims.get(CLAIM_ROLES) instanceof List<?> values) || values.isEmpty()) {
                throw new InvalidCredentialsException("Invalid access token");
            }
            List<String> roles = values.stream().map(role -> {
                if (!(role instanceof String name)) throw new InvalidCredentialsException("Invalid access token");
                String normalized = name.startsWith("ROLE_") ? name.substring(5) : name;
                return RoleType.valueOf(normalized).name();
            }).distinct().toList();
            return new AccessTokenClaims(userId, claims.getSubject(), roles, claims.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidCredentialsException("Invalid access token");
        }
    }

    public record AccessTokenClaims(long userId, String username, List<String> roles, Instant expiresAt) {}
}

