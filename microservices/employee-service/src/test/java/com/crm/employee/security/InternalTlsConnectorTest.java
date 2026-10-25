package com.crm.employee.security;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import static org.assertj.core.api.Assertions.*;

class InternalTlsConnectorTest {
    @TempDir static Path directory;
    static Path store, passwordFile;
    static SSLContext trusted;
    @BeforeAll static void certificate() throws Exception {
        store = directory.resolve("test.p12");
        passwordFile = directory.resolve("password");
        String password = "ephemeral-test-password";
        Files.writeString(passwordFile, password);
        var keytool = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool");
        var process = new ProcessBuilder(keytool.toString(), "-genkeypair", "-alias", "employee",
                "-keyalg", "RSA", "-keysize", "2048", "-validity", "2", "-dname", "CN=localhost",
                "-ext", "SAN=dns:localhost", "-keystore", store.toString(), "-storetype", "PKCS12",
                "-storepass", password).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IllegalStateException("Test TLS setup timed out"); }
        assertThat(process.exitValue()).isZero();
        var keys = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(store)) { keys.load(input, password.toCharArray()); }
        var trust = KeyStore.getInstance("PKCS12"); trust.load(null, null);
        trust.setCertificateEntry("test", keys.getCertificate("employee"));
        var managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); managers.init(trust);
        trusted = SSLContext.getInstance("TLS"); trusted.init(null, managers.getTrustManagers(), null);
    }
    @Test void disabledDoesNotAddAConnector() {
        var factory = new TomcatServletWebServerFactory();
        new InternalTlsConfiguration().internalTlsConnector(new InternalTlsConfiguration.Properties(false, null, null, null, null)).customize(factory);
        assertThat(factory.getAdditionalTomcatConnectors()).isEmpty();
    }
    @Test void missingMaterialAndSharedPortFailClosed() {
        assertThatThrownBy(() -> new InternalTlsConfiguration.Properties(true, 8443, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        var factory = new TomcatServletWebServerFactory(8443);
        assertThatThrownBy(() -> new InternalTlsConfiguration().internalTlsConnector(
                new InternalTlsConfiguration.Properties(true, 8443, store, passwordFile, null)).customize(factory)).isInstanceOf(IllegalStateException.class);
    }
    @Test void realHttpsPreservesExistingHttpAndValidatesTheCertificate() throws Exception {
        int port;
        try (var socket = new ServerSocket()) { socket.bind(new InetSocketAddress("127.0.0.1", 0)); port = socket.getLocalPort(); }
        var factory = new TomcatServletWebServerFactory(0);
        factory.setBaseDirectory(directory.resolve("tomcat").toFile());
        new InternalTlsConfiguration().internalTlsConnector(
                new InternalTlsConfiguration.Properties(true, port, store, passwordFile, null)).customize(factory);
        var server = factory.getWebServer(context -> {
            var servlet = context.addServlet("test-probe", new jakarta.servlet.http.HttpServlet() {
                @Override protected void doGet(jakarta.servlet.http.HttpServletRequest request,
                        jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
                    response.getWriter().write(request.isSecure() && request.getAttribute("jakarta.servlet.request.cipher_suite") != null ? "TLS" : "HTTP");
                }
            });
            servlet.addMapping("/probe");
        });
        try {
            server.start();
            try (var client = HttpClient.newBuilder().sslContext(trusted).connectTimeout(Duration.ofSeconds(2)).build()) {
                assertThat(client.send(HttpRequest.newBuilder(URI.create("https://localhost:" + port + "/probe")).timeout(Duration.ofSeconds(3)).build(),
                        HttpResponse.BodyHandlers.ofString()).body()).isEqualTo("TLS");
                assertThat(client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + "/probe")).timeout(Duration.ofSeconds(3)).build(),
                        HttpResponse.BodyHandlers.ofString()).body()).isEqualTo("HTTP");
                assertThatThrownBy(() -> client.send(HttpRequest.newBuilder(URI.create("https://127.0.0.1:" + port + "/probe"))
                        .timeout(Duration.ofSeconds(3)).build(), HttpResponse.BodyHandlers.ofString())).isInstanceOf(javax.net.ssl.SSLHandshakeException.class);
            }
        } finally { server.stop(); server.destroy(); }
    }
}
