package com.crm.security;

import static org.assertj.core.api.Assertions.*;
import static com.crm.security.AccountValidationResult.Status.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class AccountValidationClientTest {
    static final String SECRET = "synthetic-common-security-test-key-32-bytes";
    static final JwtService JWT = new JwtService(SECRET);
    static AccountValidationProperties properties() {
        return new AccountValidationProperties(URI.create("http://localhost:12345"), null, null, null);
    }
    static String token(Object uid, Object roles, Boolean enabled, String subject, Instant expiration) {
        return Jwts.builder().subject(subject).claim("uid", uid).claim("roles", roles)
                .claim("enabled", enabled).expiration(expiration == null ? null : Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
    static String bearer() {
        return "Bearer " + token("42", List.of("EMPLOYEE", "ROLE_ADMIN"), true, "alice", Instant.now().plusSeconds(60));
    }

    @Test void successPreservesSignedIdentityRolesAndBearerWithoutCache() {
        AtomicInteger calls = new AtomicInteger();
        String original = bearer();
        try (var client = new AccountValidationClient(JWT, properties(), header -> {
            assertThat(header).isEqualTo(original);
            return calls.incrementAndGet() == 1 ? 204 : 401;
        }, Clock.systemUTC())) {
            var result = client.validate(original);
            assertThat(result.status()).isEqualTo(VALID);
            assertThat(result.claims().userId()).isEqualTo(42);
            assertThat(result.claims().username()).isEqualTo("alice");
            assertThat(result.claims().roles()).containsExactly("EMPLOYEE", "ADMIN");
            assertThat(client.validate(original).status()).isEqualTo(INVALID);
            assertThat(calls).hasValue(2);
        }
    }

    @ParameterizedTest @ValueSource(ints = {200, 201, 301, 302, 307, 308, 403, 404, 429, 500, 503})
    void only204ConfirmsValidity(int status) {
        try (var client = new AccountValidationClient(JWT, properties(), header -> status, Clock.systemUTC())) {
            assertThat(client.validate(bearer()).status()).isEqualTo(UNAVAILABLE);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"dns", "refused", "timeout"})
    void transportFailuresRemainAvailabilityFailures(String kind) {
        try (var client = new AccountValidationClient(JWT, properties(), header -> {
            throw switch (kind) {
                case "dns" -> new UnknownHostException("synthetic.invalid");
                case "refused" -> new ConnectException("connection refused");
                default -> new SocketTimeoutException("read timeout");
            };
        }, Clock.systemUTC())) {
            assertThat(client.validate(bearer()).status()).isEqualTo(UNAVAILABLE);
        }
    }

    @Test void globalDeadlineBoundsBlockedWorkAndSaturationFailsClosed() throws Exception {
        var props = new AccountValidationProperties(properties().baseUrl(), Duration.ofMillis(20),
                Duration.ofMillis(20), Duration.ofMillis(40));
        var release = new java.util.concurrent.CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        try (var client = new AccountValidationClient(JWT, props, header -> {
            calls.incrementAndGet();
            while (release.getCount() != 0) {
                try { release.await(); } catch (InterruptedException ignored) { /* emulate blocked DNS */ }
            }
            return 204;
        }, Clock.systemUTC())) {
            long start = System.nanoTime();
            for (int i = 0; i < 6; i++) assertThat(client.validate(bearer()).status()).isEqualTo(UNAVAILABLE);
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
            assertThat(calls).hasValue(4);
        } finally { release.countDown(); }
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"0", "-1", "01", " 1", "1.0", "9223372036854775808", "abc", ""})
    void invalidUidIsRejectedBeforeNetwork(String uid) {
        assertLocalRejection(token(uid, List.of("CLIENT"), true, "alice", Instant.now().plusSeconds(60)));
    }
    @Test void numericUidIsRejected() {
        assertLocalRejection(token(42, List.of("CLIENT"), true, "alice", Instant.now().plusSeconds(60)));
    }
    @Test void malformedWrongSignatureAndExpiredAreRejected() {
        assertLocalRejection("invalid-token");
        assertLocalRejection(new JwtService("different-synthetic-test-secret-32-bytes")
                .generateToken("alice", "", true, List.of("CLIENT")));
        assertLocalRejection(token("42", List.of("CLIENT"), true, "alice", Instant.now().minusSeconds(5)));
    }
    @Test void mandatoryClaimsAreStrict() {
        for (Boolean enabled : Arrays.asList(null, false))
            assertLocalRejection(token("42", List.of("CLIENT"), enabled, "alice", Instant.now().plusSeconds(60)));
        for (String subject : Arrays.asList(null, " "))
            assertLocalRejection(token("42", List.of("CLIENT"), true, subject, Instant.now().plusSeconds(60)));
        assertLocalRejection(token("42", List.of("CLIENT"), true, "alice", null));
        for (Object roles : List.of(List.of(), List.of(1), List.of("ROOT"), "ADMIN", List.of("ROLE_ROLE_ADMIN")))
            assertLocalRejection(token("42", roles, true, "alice", Instant.now().plusSeconds(60)));
    }
    @Test void missingBearerAndHeaderInjectionAreRejected() {
        try (var client = new AccountValidationClient(JWT, properties(), h -> { throw new AssertionError("network called"); }, Clock.systemUTC())) {
            for (String header : Arrays.asList(null, "Basic abc", "Bearer abc\r\nHost: evil"))
                assertThat(client.validate(header).status()).isEqualTo(INVALID);
        }
    }
    @Test void tokenExpiringDuringRemoteCallIsRejected() {
        try (var client = new AccountValidationClient(JWT, properties(), h -> 204,
                Clock.fixed(Instant.now().plusSeconds(120), ZoneOffset.UTC))) {
            assertThat(client.validate(bearer()).status()).isEqualTo(INVALID);
        }
    }
    @Test void interruptedCallerDoesNotAuthenticate() {
        try (var client = new AccountValidationClient(JWT, properties(), h -> { Thread.sleep(500); return 204; }, Clock.systemUTC())) {
            Thread.currentThread().interrupt();
            assertThat(client.validate(bearer()).status()).isEqualTo(UNAVAILABLE);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally { Thread.interrupted(); }
    }
    private void assertLocalRejection(String token) {
        try (var client = new AccountValidationClient(JWT, properties(), h -> { throw new AssertionError("network called"); }, Clock.systemUTC())) {
            assertThat(client.validate("Bearer " + token).status()).isEqualTo(INVALID);
        }
    }
}
