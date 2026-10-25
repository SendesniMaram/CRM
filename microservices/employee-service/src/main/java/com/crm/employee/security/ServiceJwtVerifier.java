package com.crm.employee.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

public final class ServiceJwtVerifier {
    public static final String AUTHORITY = "SERVICE_employee:profile-summary";
    private static final Set<String> CLAIMS = Set.of("iss", "sub", "aud", "permission", "employee_id", "method", "iat", "nbf", "exp", "jti");
    private final Map<String, RSAPublicKey> keys;
    private final Clock clock;
    private final ObjectMapper json = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public ServiceJwtVerifier(Map<String, RSAPublicKey> keys, Clock clock) {
        if (keys == null || keys.size() > 8 || keys.entrySet().stream().anyMatch(e ->
                !e.getKey().matches("[A-Za-z0-9_-]{1,64}") || e.getValue() == null || e.getValue().getModulus().bitLength() < 2048)) {
            throw new IllegalArgumentException("Invalid trusted service keys");
        }
        this.keys = Map.copyOf(keys); this.clock = java.util.Objects.requireNonNull(clock);
    }
    public boolean configured() { return !keys.isEmpty(); }

    public AuthenticatedServicePrincipal verify(String token, String method, String employeeId) {
        JsonNode body;
        Instant expires;
        try {
            if (token == null || token.length() > 8192 || !token.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")) throw invalid();
            String[] parts = token.split("\\.");
            JsonNode header = decode(parts[0]);
            if (!fields(header).equals(Set.of("alg", "typ", "kid")) || !text(header, "alg", "RS256")
                    || !text(header, "typ", "crm-service+jwt") || !header.path("kid").isTextual()) throw invalid();
            var key = keys.get(header.path("kid").textValue());
            if (key == null) throw invalid();
            // The protected header is restricted to RS256 above, before the same immutable token is verified.
            Jwts.parser().verifyWith(key)
                    .clock(() -> Date.from(clock.instant())).build().parseSignedClaims(token);
            body = decode(parts[1]);
            if (!fields(body).equals(CLAIMS) || !text(body, "iss", "urn:crm:service") || !text(body, "sub", "hr-service")
                    || !employeeAudience(body.path("aud"))
                    || !canonical(body.path("employee_id").textValue()) || !body.path("jti").isTextual()
                    || !UUID.fromString(body.path("jti").textValue()).toString().equals(body.path("jti").textValue())) throw invalid();
            for (String name : Set.of("iat", "nbf", "exp")) {
                if (!body.path(name).isIntegralNumber() || !body.path(name).canConvertToLong()) throw invalid();
            }
            long issued = body.path("iat").longValue(), active = body.path("nbf").longValue(), end = body.path("exp").longValue();
            if (issued < 0 || active != issued || end <= issued || Math.subtractExact(end, issued) > 30) throw invalid();
            var now = clock.instant(); expires = Instant.ofEpochSecond(end);
            if (Instant.ofEpochSecond(issued).isAfter(now) || !expires.isAfter(now)) throw invalid();
        } catch (Exception ex) {
            throw invalid();
        }
        if (!canonical(employeeId)) throw new IllegalArgumentException("Canonical Employee identifier required");
        if (!text(body, "permission", "employee:profile-summary") || !text(body, "method", "GET")
                || !"GET".equals(method) || !body.path("employee_id").textValue().equals(employeeId)) {
            throw new AccessDeniedException("Service access denied");
        }
        return new AuthenticatedServicePrincipal("hr-service", employeeId, body.path("jti").textValue(), expires);
    }
    private JsonNode decode(String part) throws Exception {
        return json.readTree(new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8));
    }
    private static Set<String> fields(JsonNode value) {
        if (value == null || !value.isObject()) throw invalid();
        var fields = new java.util.HashSet<String>(); value.fieldNames().forEachRemaining(fields::add); return fields;
    }
    private static boolean text(JsonNode value, String name, String expected) {
        return value.path(name).isTextual() && expected.equals(value.path(name).textValue());
    }
    private static boolean employeeAudience(JsonNode audience) {
        // JWT permits a single audience as a string; JJWT emits that representation for a singleton.
        return audience.isTextual() ? audience.textValue().equals("employee-service")
                : audience.isArray() && audience.size() == 1 && audience.get(0).isTextual()
                    && audience.get(0).textValue().equals("employee-service");
    }
    private static boolean canonical(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) return false;
        try { return Long.parseLong(value) > 0; } catch (NumberFormatException ex) { return false; }
    }
    private static BadCredentialsException invalid() { return new BadCredentialsException("Invalid service credential"); }
}
