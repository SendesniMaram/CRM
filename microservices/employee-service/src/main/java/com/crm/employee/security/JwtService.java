package com.crm.employee.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

/**
 * Minimal JWT service for employee-service.
 */
public class JwtService {

    private final SecretKey secretKey;

    public JwtService() {
        this.secretKey = Keys.hmacShaKeyFor(
                "dev-change-me-dev-change-me-dev-change-me-dev-change-me".getBytes(StandardCharsets.UTF_8)
        );
    }

    public JwtService(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String email, boolean enabled) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 1000L * 60 * 60);

        return Jwts.builder()
                .subject(username)
                .claim("email", email)
                .claim("enabled", enabled)
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
        } catch (JwtException e) {
            return null;
        }
    }

    public boolean isTokenValid(String token, String username) {
        String extracted = extractUsername(token);
        return extracted != null && extracted.equals(username);
    }
}
