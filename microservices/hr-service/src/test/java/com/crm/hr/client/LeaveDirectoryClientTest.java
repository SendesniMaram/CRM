package com.crm.hr.client;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaveDirectoryClientTest {
    HttpServer server;
    DiscoveryClient discovery;
    LeaveDirectoryClient client;

    @BeforeEach void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        discovery = mock(DiscoveryClient.class);
        when(discovery.getInstances("identity-service")).thenReturn(List.of(
                new DefaultServiceInstance("test", "identity-service", "127.0.0.1", server.getAddress().getPort(), false)));
        client = new LeaveDirectoryClient(discovery);
    }

    @AfterEach void close() { server.stop(0); }

    void respond(int status, String body) {
        server.createContext("/api/users/1", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    @ParameterizedTest
    @CsvSource({"401,502", "403,403", "404,404", "400,502", "500,503", "503,503"})
    void distinguishesRemoteFailuresWithoutExposingResponse(int remote, int expected) {
        respond(remote, "{\"internal\":\"private remote detail\"}");
        var ex = assertThrows(ResponseStatusException.class,
                () -> client.read("identity-service", "/api/users/1", "Bearer test-only"));
        assertEquals(expected, ex.getStatusCode().value());
        assertFalse(ex.getReason().contains("private remote detail"));
        assertFalse(ex.getReason().contains("Bearer"));
    }

    @Test void discoveryAbsenceIsUnavailable() {
        when(discovery.getInstances("identity-service")).thenReturn(List.of());
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> client.read("identity-service", "/api/users/1", "Bearer test-only")).getStatusCode().value());
    }

    @Test void connectionFailureIsUnavailable() {
        server.stop(0);
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> client.read("identity-service", "/api/users/1", "Bearer test-only")).getStatusCode().value());
    }

    @Test void emptyResponseIsNotSuccessfulValidation() {
        respond(200, "");
        assertEquals(502, assertThrows(ResponseStatusException.class,
                () -> client.read("identity-service", "/api/users/1", "Bearer test-only")).getStatusCode().value());
    }

    @Test void successReadsJsonAndForwardsAuthorization() {
        server.createContext("/api/users/1", exchange -> {
            String bearer = exchange.getRequestHeaders().getFirst("Authorization");
            byte[] bytes = "{\"id\":1}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders("Bearer test-only".equals(bearer) ? 200 : 403, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        assertEquals(1, client.read("identity-service", "/api/users/1", "Bearer test-only").path("id").asInt());
    }
}
