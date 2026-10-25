package com.crm.hr.client;

import com.crm.hr.exception.EmployeeProfileException;
import com.crm.hr.security.EmployeeServiceTokenSigner;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class EmployeeProfileClientTest {
    static EmployeeServiceTokenSigner signer;
    static final String BODY = "{\"employeeId\":\"103\",\"firstName\":\"Test\",\"lastName\":\"Employee\",\"jobTitle\":\"Developer\",\"departmentId\":\"7\",\"hireDate\":\"2026-01-15\"}";
    @BeforeAll static void key() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        signer = new EmployeeServiceTokenSigner((RSAPrivateKey) generator.generateKeyPair().getPrivate(), "test", Clock.systemUTC(), Duration.ofSeconds(30));
    }
    static EmployeeProfileClientProperties properties() { return new EmployeeProfileClientProperties(true, URI.create("https://employee.test"), null, null, null); }
    static EmployeeProfileClient.Reply reply(int status, String body) { return new EmployeeProfileClient.Reply(status, body.getBytes(StandardCharsets.UTF_8), "application/json"); }
    static void failure(Runnable action, int status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(EmployeeProfileException.class, ex -> assertThat(ex.status()).isEqualTo(status));
    }
    @Test void validatesTheCompleteMinimalDto() {
        try (var client = new EmployeeProfileClient(properties(), signer, (id, token) -> reply(200, BODY), null)) {
            var profile = client.read("103"); assertThat(profile.employeeId()).isEqualTo("103");
            assertThat(profile.departmentId()).isEqualTo("7"); assertThat(profile.firstName()).isEqualTo("Test");
        }
    }
    @Test void nullableFieldsAndLongIdsStayExact() {
        String body = "{\"employeeId\":\"9223372036854775807\",\"firstName\":null,\"lastName\":null,\"jobTitle\":null,\"departmentId\":\"9007199254740993\",\"hireDate\":null}";
        try (var client = new EmployeeProfileClient(properties(), signer, (id, token) -> reply(200, body), null)) {
            var profile = client.read("9223372036854775807"); assertThat(profile.departmentId()).isEqualTo("9007199254740993"); assertThat(profile.hireDate()).isNull();
        }
    }
    @ParameterizedTest @ValueSource(ints = {401, 403, 500, 502, 503})
    void dependencyAuthenticationAndServerFailuresBecome503WithoutRetry(int status) {
        var calls = new AtomicInteger();
        try (var client = new EmployeeProfileClient(properties(), signer, (id, token) -> { calls.incrementAndGet(); return reply(status, "private diagnostic"); }, null)) {
            failure(() -> client.read("103"), 503); assertThat(calls).hasValue(1);
        }
    }
    @Test void missingEmployeeIs404() {
        try (var client = new EmployeeProfileClient(properties(), signer, (id, token) -> reply(404, "private diagnostic"), null)) { failure(() -> client.read("103"), 404); }
    }
    @ParameterizedTest @ValueSource(ints = {201, 204, 301, 302, 307, 308, 400, 429})
    void unexpectedProtocolStatusIs502(int status) {
        try (var client = new EmployeeProfileClient(properties(), signer, (id, token) -> reply(status, ""), null)) { failure(() -> client.read("103"), 502); }
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "null", "{", "[]", "duplicate", "mismatch", "numeric", "salary", "date", "department", "trailing"})
    void invalidResponseNeverBecomesAProfile(String kind) {
        String body = switch (kind) {
            case "duplicate" -> BODY.replace("\"employeeId\":\"103\"", "\"employeeId\":\"103\",\"employeeId\":\"104\"");
            case "mismatch" -> BODY.replace("103", "104");
            case "numeric" -> BODY.replace("\"103\"", "103");
            case "salary" -> BODY.replace("\"firstName\"", "\"salary\":123,\"firstName\"");
            case "date" -> BODY.replace("2026-01-15", "2026-02-30");
            case "department" -> BODY.replace("\"7\"", "\"01\"");
            case "trailing" -> BODY + "{}";
            default -> kind;
        };
        try (var client = new EmployeeProfileClient(properties(), signer, (id, token) -> reply(200, body), null)) { failure(() -> client.read("103"), 502); }
    }
    @Test void wrongContentTypeAndOversizedBodiesAreRefused() {
        try (var client = new EmployeeProfileClient(properties(), signer, (id, token) -> new EmployeeProfileClient.Reply(200, BODY.getBytes(StandardCharsets.UTF_8), "text/html"), null)) { failure(() -> client.read("103"), 502); }
        try (var client = new EmployeeProfileClient(properties(), signer, (id, token) -> reply(200, "x".repeat(16385)), null)) { failure(() -> client.read("103"), 502); }
    }
    @Test void missingConfigurationIsClosed() {
        try (var client = new EmployeeProfileClient(new EmployeeProfileClientProperties(false, null, null, null, null), null, null, null)) { failure(() -> client.read("103"), 503); }
    }
    @Test void globalDeadlineAndSaturationAreBoundedEvenForNonInterruptibleTransport() {
        var release = new CountDownLatch(1); var calls = new AtomicInteger();
        var properties = new EmployeeProfileClientProperties(true, URI.create("https://employee.test"), Duration.ofMillis(10), Duration.ofMillis(10), Duration.ofMillis(50));
        try (var client = new EmployeeProfileClient(properties, signer, (id, token) -> {
            calls.incrementAndGet();
            while (release.getCount() > 0) try { release.await(); } catch (InterruptedException ignored) { /* Simulated stuck DNS. */ }
            return reply(200, BODY);
        }, null)) {
            for (int i = 0; i < 4; i++) failure(() -> client.read("103"), 503);
            long start = System.nanoTime(); failure(() -> client.read("103"), 503);
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1)); assertThat(calls).hasValue(4);
        } finally { release.countDown(); }
    }
    @Test void transportDiagnosticsAndCredentialsAreNotRetainedInErrors() {
        try (var client = new EmployeeProfileClient(properties(), signer, (id, token) -> { throw new java.io.IOException("private credential diagnostic"); }, null)) {
            assertThatThrownBy(() -> client.read("103")).isInstanceOfSatisfying(EmployeeProfileException.class, ex -> {
                assertThat(ex.getMessage()).doesNotContain("private credential diagnostic"); assertThat(ex.getCause()).isNull();
            });
        }
    }
}
