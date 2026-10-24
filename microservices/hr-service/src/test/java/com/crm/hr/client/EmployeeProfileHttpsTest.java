package com.crm.hr.client;

import com.crm.hr.security.EmployeeServiceTokenSigner;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;
import io.jsonwebtoken.Jwts;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.apache.hc.core5.ssl.SSLContexts;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class EmployeeProfileHttpsTest {
    @TempDir static Path directory;
    static HttpsServer server;
    static SSLContext trusted;
    static KeyPair jwtKey;
    static EmployeeServiceTokenSigner signer;
    static final AtomicInteger calls = new AtomicInteger();
    static final AtomicInteger status = new AtomicInteger(200);
    static final AtomicReference<String> bearer = new AtomicReference<>();
    static final AtomicReference<String> path = new AtomicReference<>();
    @BeforeAll static void https() throws Exception {
        Path store = directory.resolve("ephemeral.p12"); String password = "test-only-password";
        Path keytool = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool");
        var process = new ProcessBuilder(keytool.toString(), "-genkeypair", "-alias", "test", "-keyalg", "RSA", "-keysize", "2048", "-validity", "2",
                "-dname", "CN=localhost", "-ext", "SAN=dns:localhost", "-keystore", store.toString(), "-storetype", "PKCS12", "-storepass", password, "-keypass", password)
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IllegalStateException("Ephemeral TLS setup timed out"); }
        assertThat(process.exitValue()).isZero();
        var keys = KeyStore.getInstance("PKCS12"); try (var input = Files.newInputStream(store)) { keys.load(input, password.toCharArray()); }
        Files.writeString(directory.resolve("employee.pem"), "-----BEGIN CERTIFICATE-----\n"
                + java.util.Base64.getMimeEncoder(64, new byte[]{10}).encodeToString(keys.getCertificate("test").getEncoded())
                + "\n-----END CERTIFICATE-----\n");
        var managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); managers.init(keys, password.toCharArray());
        var serverTls = SSLContext.getInstance("TLS"); serverTls.init(managers.getKeyManagers(), null, null);
        var trust = KeyStore.getInstance("PKCS12"); trust.load(null, null); trust.setCertificateEntry("test", keys.getCertificate("test"));
        trusted = SSLContexts.custom().loadTrustMaterial(trust, null).build();
        var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); jwtKey = generator.generateKeyPair();
        signer = new EmployeeServiceTokenSigner((RSAPrivateKey) jwtKey.getPrivate(), "test", Clock.systemUTC(), Duration.ofSeconds(30));
        server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0); server.setHttpsConfigurator(new HttpsConfigurator(serverTls));
        server.createContext("/", request -> {
            calls.incrementAndGet(); bearer.set(request.getRequestHeaders().getFirst("Authorization")); path.set(request.getRequestURI().toString());
            byte[] body = EmployeeProfileClientTest.BODY.getBytes(StandardCharsets.UTF_8);
            request.getResponseHeaders().set("Content-Type", "application/json");
            request.getResponseHeaders().set("Location", "https://localhost:" + server.getAddress().getPort() + "/redirected");
            request.sendResponseHeaders(status.get(), body.length); request.getResponseBody().write(body); request.close();
        }); server.start();
    }
    @AfterAll static void stop() { if (server != null) server.stop(0); }
    @BeforeEach void reset() { calls.set(0); status.set(200); bearer.set(null); path.set(null); }
    EmployeeProfileClientProperties properties(String host) {
        return new EmployeeProfileClientProperties(true, URI.create("https://" + host + ":" + server.getAddress().getPort()), Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(2));
    }
    @Test void trustedCertificateAndHostnameCarryOnlyTheDedicatedResourceBoundToken() {
        try (var client = EmployeeProfileClientConfiguration.create(properties("localhost"), signer, trusted)) {
            assertThat(client.read("103").employeeId()).isEqualTo("103");
            var claims = Jwts.parser().verifyWith(jwtKey.getPublic()).build().parseSignedClaims(bearer.get().substring(7)).getPayload();
            assertThat(claims.getSubject()).isEqualTo("hr-service"); assertThat(claims.get("employee_id")).isEqualTo("103");
            assertThat(claims.get("method")).isEqualTo("GET"); assertThat(claims.get("permission")).isEqualTo("employee:profile-summary");
            assertThat(claims).doesNotContainKeys("uid", "roles"); assertThat(calls).hasValue(1);
            assertThat(path.get()).isEqualTo("/api/internal/employees/103/profile-summary");
        }
    }
    @Test void trustedCertificateWithWrongHostnameIsStillRejected() {
        try (var client = EmployeeProfileClientConfiguration.create(properties("127.0.0.1"), signer, trusted)) { EmployeeProfileClientTest.failure(() -> client.read("103"), 503); assertThat(calls).hasValue(0); }
    }
    @Test void unknownCertificateAuthorityIsRejected() {
        try (var client = EmployeeProfileClientConfiguration.create(properties("localhost"), signer, SSLContexts.createDefault())) { EmployeeProfileClientTest.failure(() -> client.read("103"), 503); assertThat(calls).hasValue(0); }
    }
    @Test void externalCertificateConfigurationPreservesHostnameVerification() {
        var p = new EmployeeProfileClientProperties(true, properties("localhost").baseUrl(), Duration.ofSeconds(1),
                Duration.ofSeconds(1), Duration.ofSeconds(2), directory.resolve("employee.pem"));
        try (var client = EmployeeProfileClientConfiguration.create(p, signer, EmployeeProfileClientConfiguration.trust(p))) {
            assertThat(client.read("103").employeeId()).isEqualTo("103");
        }
        var wrong = new EmployeeProfileClientProperties(true, properties("127.0.0.1").baseUrl(), Duration.ofSeconds(1),
                Duration.ofSeconds(1), Duration.ofSeconds(2), directory.resolve("employee.pem"));
        try (var client = EmployeeProfileClientConfiguration.create(wrong, signer, EmployeeProfileClientConfiguration.trust(wrong))) {
            EmployeeProfileClientTest.failure(() -> client.read("103"), 503);
        }
    }
    @Test void malformedCertificateFailsWithoutLeakingItsContent() throws Exception {
        Path invalid = directory.resolve("invalid.pem"); Files.writeString(invalid, "not-a-certificate");
        var p = new EmployeeProfileClientProperties(true, properties("localhost").baseUrl(), Duration.ofSeconds(1),
                Duration.ofSeconds(1), Duration.ofSeconds(2), invalid);
        assertThatThrownBy(() -> EmployeeProfileClientConfiguration.trust(p)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Employee TLS trust could not be configured").hasNoCause();
    }
    @Test void redirectsAndServerFailuresAreNotRetried() {
        status.set(302);
        try (var client = EmployeeProfileClientConfiguration.create(properties("localhost"), signer, trusted)) { EmployeeProfileClientTest.failure(() -> client.read("103"), 502); assertThat(calls).hasValue(1); }
        calls.set(0); status.set(503);
        try (var client = EmployeeProfileClientConfiguration.create(properties("localhost"), signer, trusted)) { EmployeeProfileClientTest.failure(() -> client.read("103"), 503); assertThat(calls).hasValue(1); }
    }
}
