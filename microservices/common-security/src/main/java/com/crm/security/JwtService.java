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

    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ENABLED = "enabled";

    private static final long ONE_HOUR_MS = 1000L * 60 * 60;
    private static final String SECRET_ENV_VAR = "APP_JWT_SECRET";
    public static final String DEFAULT_SECRET = "crm-secret-key-0123456789abcdef0123456789abcdef";

    private final SecretKey secretKey;

    public JwtService(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(resolveSecret(secret).getBytes(StandardCharsets.UTF_8));
    }

    public JwtService() {
        this(resolveSecretFromEnvironment());
    }

    /**
     * Returns a valid HS256 secret, falling back to {@link #DEFAULT_SECRET} when the
     * configured value is null, blank or too short to be a secure HMAC key.
     */
    private static String resolveSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return DEFAULT_SECRET;
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            return DEFAULT_SECRET;
        }
        return secret;
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
     * True if the token has a valid signature and is not expired.
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date());
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

