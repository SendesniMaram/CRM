package com.crm.hr.security;

import io.jsonwebtoken.Jwts;
import java.security.interfaces.RSAPrivateKey;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/** Issues a separate service credential; never reads APP_JWT_SECRET or a user's bearer. No HTTP calls. */
public final class EmployeeServiceTokenSigner {
    private final RSAPrivateKey key;
    private final String keyId;
    private final Clock clock;
    private final Duration lifetime;

    public EmployeeServiceTokenSigner(RSAPrivateKey key, String keyId, Clock clock, Duration lifetime) {
        if (key == null || key.getModulus().bitLength() < 2048 || keyId == null
                || !keyId.matches("[A-Za-z0-9_-]{1,64}") || lifetime == null
                || lifetime.isNegative() || lifetime.isZero() || lifetime.getNano() != 0
                || lifetime.compareTo(Duration.ofSeconds(30)) > 0) {
            throw new IllegalArgumentException("Invalid HR service signing configuration");
        }
        this.key = key; this.keyId = keyId; this.clock = Objects.requireNonNull(clock); this.lifetime = lifetime;
    }

    public String signForEmployee(String employeeId) {
        if (employeeId == null || !employeeId.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("Canonical Employee identifier required");
        }
        try { Long.parseLong(employeeId); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Employee identifier outside supported range"); }
        var now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        return Jwts.builder().header().type("crm-service+jwt").keyId(keyId).and()
                .issuer("urn:crm:service").subject("hr-service").audience().add("employee-service").and()
                .claim("permission", "employee:profile-summary").claim("employee_id", employeeId).claim("method", "GET")
                .issuedAt(Date.from(now)).notBefore(Date.from(now)).expiration(Date.from(now.plus(lifetime)))
                .id(UUID.randomUUID().toString()).signWith(key, Jwts.SIG.RS256).compact();
    }
}
