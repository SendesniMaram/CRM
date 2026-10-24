package com.crm.security;

import static org.assertj.core.api.Assertions.*;
import java.net.*;
import java.time.Duration;
import java.util.concurrent.atomic.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class AccountValidationHttpTest {
    private AccountValidationProperties properties(HttpServer server) {
        return new AccountValidationProperties(URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                Duration.ofMillis(100), Duration.ofMillis(100), Duration.ofMillis(300));
    }
    @ParameterizedTest @ValueSource(ints = {204, 401, 200, 403, 404, 429, 500, 503})
    void actualHttpContract(int status) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> bearer = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/api/internal/access-token/validate", request -> {
            bearer.set(request.getRequestHeaders().getFirst("Authorization"));
            calls.incrementAndGet();
            request.sendResponseHeaders(status, -1);
            request.close();
        });
        server.start();
        try (var client = new AccountValidationClient(AccountValidationClientTest.JWT, properties(server))) {
            String original = AccountValidationClientTest.bearer();
            var expected = status == 204 ? AccountValidationResult.Status.VALID
                    : status == 401 ? AccountValidationResult.Status.INVALID : AccountValidationResult.Status.UNAVAILABLE;
            assertThat(client.validate(original).status()).isEqualTo(expected);
            assertThat(bearer.get()).isEqualTo(original);
            assertThat(calls).hasValue(1);
        } finally { server.stop(0); }
    }
    @Test void redirectDoesNotSendBearerToAnotherOrigin() throws Exception {
        var target = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var source = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger targetCalls = new AtomicInteger();
        target.createContext("/", request -> { targetCalls.incrementAndGet(); request.sendResponseHeaders(204, -1); request.close(); });
        source.createContext("/", request -> {
            request.getResponseHeaders().add("Location", "http://127.0.0.1:" + target.getAddress().getPort() + "/");
            request.sendResponseHeaders(302, -1); request.close();
        });
        source.start(); target.start();
        try (var client = new AccountValidationClient(AccountValidationClientTest.JWT, properties(source))) {
            assertThat(client.validate(AccountValidationClientTest.bearer()).status()).isEqualTo(AccountValidationResult.Status.UNAVAILABLE);
            assertThat(targetCalls).hasValue(0);
        } finally { source.stop(0); target.stop(0); }
    }
    @Test void slowHttpResponseIsUnavailable() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", request -> {
            try { Thread.sleep(400); request.sendResponseHeaders(204, -1); }
            catch (Exception ignored) { } finally { request.close(); }
        });
        server.start();
        try (var client = new AccountValidationClient(AccountValidationClientTest.JWT, properties(server))) {
            assertThat(client.validate(AccountValidationClientTest.bearer()).status()).isEqualTo(AccountValidationResult.Status.UNAVAILABLE);
        } finally { server.stop(0); }
    }
    @Test void closedLocalPortIsUnavailable() throws Exception {
        int port;
        try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) { port = socket.getLocalPort(); }
        var props = new AccountValidationProperties(URI.create("http://127.0.0.1:" + port), null, null, null);
        try (var client = new AccountValidationClient(AccountValidationClientTest.JWT, props)) {
            assertThat(client.validate(AccountValidationClientTest.bearer()).status()).isEqualTo(AccountValidationResult.Status.UNAVAILABLE);
        }
    }
}
