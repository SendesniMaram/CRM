package com.crm.payroll.security;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Test-only HTTP Identity; real production client, parser and filter remain active. */
public abstract class TestIdentitySupport {
    private static final String KEY = "test-only-secret-key-not-for-production-crm2024";
    public static volatile int identityStatus = 204;
    public static volatile boolean enabled = true, locked = false, roleRemoved = false;
    public static volatile long delayMillis;
    public static final AtomicInteger validations = new AtomicInteger();
    public static final AtomicReference<String> lastBearer = new AtomicReference<>();
    private static final HttpServer SERVER = startServer();
    private static HttpServer startServer() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            var executor = Executors.newCachedThreadPool(task -> {
                var thread = new Thread(task, "synthetic-identity"); thread.setDaemon(true); return thread;
            });
            server.setExecutor(executor);
            server.createContext("/api/internal/access-token/validate", request -> {
                validations.incrementAndGet();
                lastBearer.set(request.getRequestHeaders().getFirst("Authorization"));
                int status = !enabled || locked || roleRemoved ? 401 : identityStatus;
                try {
                    if (delayMillis > 0) Thread.sleep(delayMillis);
                    request.getResponseHeaders().set("Cache-Control", "no-store");
                    request.sendResponseHeaders(status, -1);
                } catch (Exception ignored) { } finally { request.close(); }
            });
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { server.stop(0); executor.shutdownNow(); }));
            return server;
        } catch (java.io.IOException ex) { throw new IllegalStateException("Synthetic Identity could not start", ex); }
    }
    @DynamicPropertySource
    static void identityProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.account-validation.base-url",
                () -> "http://127.0.0.1:" + SERVER.getAddress().getPort());
    }
    @BeforeEach void resetSyntheticIdentity() {
        identityStatus = 204; enabled = true; locked = false; roleRemoved = false;
        delayMillis = 0; validations.set(0); lastBearer.set(null);
    }
    public static String token(String username, String email, boolean active, Collection<String> roles) {
        return signed(username, active, roles, "42", Instant.now().plusSeconds(3600));
    }
    public static String signed(String username, boolean active, Collection<String> roles, Object uid, Instant expiration) {
        return Jwts.builder().subject(username).claim("email", "")
                .claim("uid", uid).claim("enabled", active).claim("roles", roles)
                .issuedAt(new Date()).expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
