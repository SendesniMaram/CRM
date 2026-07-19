package com.crm.identity.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

/**
 * Minimal JWT service (no custom filter).
 */
public class JwtService {

    // For now a hardcoded secret to keep things functional.
    // Later this should be externalized.
    private final SecretKey secretKey;

    public JwtService() {
        // Must be >= 256 bits for HS256 according to JWA.
        this.secretKey = Keys.hmacShaKeyFor(
                "dev-change-me-dev-change-me-dev-change-me-dev-change-me".getBytes(StandardCharsets.UTF_8)
        );
    }


    public JwtService(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String email, boolean enabled) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 1000L * 60 * 60); // 1 hour

        return Jwts.builder()
                .setSubject(username)
                .addClaims(Map.of("email", email, "enabled", enabled))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();

        } catch (JwtException e) {
            return null;
        }
    }

    public boolean isTokenValid(String token, String username) {
        String extracted = extractUsername(token);
        return extracted != null && extracted.equals(username);
    }
}

