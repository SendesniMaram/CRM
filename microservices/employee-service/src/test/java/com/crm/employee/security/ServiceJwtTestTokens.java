package com.crm.employee.security;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** Fresh keys only, no embedded private fixture or production credential. */
final class ServiceJwtTestTokens {
    static final KeyPair HR = pair();
    static final KeyPair OTHER = pair();
    static KeyPair pair() {
        try { var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); return generator.generateKeyPair(); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    static JwtBuilder builder(Instant now) {
        return Jwts.builder().header().type("crm-service+jwt").keyId("test-key").and()
                .issuer("urn:crm:service").subject("hr-service").audience().add("employee-service").and()
                .claim("permission", "employee:profile-summary").claim("employee_id", "103").claim("method", "GET")
                .issuedAt(Date.from(now)).notBefore(Date.from(now)).expiration(Date.from(now.plusSeconds(30)))
                .id(UUID.randomUUID().toString());
    }
    static String valid() { return builder(Instant.now().minusSeconds(1)).signWith(HR.getPrivate(), Jwts.SIG.RS256).compact(); }
}
