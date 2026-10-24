package com.crm.security;

import static org.assertj.core.api.Assertions.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Diagnostic loopback measurements, not an Identity/MySQL production benchmark. */
class AccountValidationPerformanceTest {
    @Test void measureLocalParsingAndOneOrTwoIndependentHttpValidations() throws Exception {
        try (var backend = new Backend(); var client = backend.client()) {
            String bearer = AccountValidationClientTest.bearer();
            var metrics = new LinkedHashMap<String, Object>();
            metrics.put("jwtLocal", measure(200, () -> AccountValidationClientTest.JWT.parseAccessToken(bearer.substring(7))));
            int before = backend.hits.get();
            metrics.put("oneService", measure(100, () -> assertThat(client.validate(bearer).status()).isEqualTo(AccountValidationResult.Status.VALID)));
            assertThat(backend.hits.get() - before).isEqualTo(110); // 10 warmups + 100 measured requests
            before = backend.hits.get();
            metrics.put("twoServicesSequential", measure(50, () -> {
                assertThat(client.validate(bearer).status()).isEqualTo(AccountValidationResult.Status.VALID);
                assertThat(client.validate(bearer).status()).isEqualTo(AccountValidationResult.Status.VALID);
            }));
            assertThat(backend.hits.get() - before).isEqualTo(120); // 10 warmup pairs + 50 measured pairs
            Path output = Path.of("target/lot-f-performance.json");
            Files.createDirectories(output.getParent());
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.toFile(), metrics);
        }
    }

    @Test void measureTimeoutAndRecoveryWithoutCachedSuccess() throws Exception {
        try (var backend = new Backend(); var client = backend.client()) {
            String bearer = AccountValidationClientTest.bearer();
            assertThat(client.validate(bearer).status()).isEqualTo(AccountValidationResult.Status.VALID);
            backend.status.set(503);
            assertThat(client.validate(bearer).status()).isEqualTo(AccountValidationResult.Status.UNAVAILABLE);
            backend.status.set(204);
            backend.delayMillis = 1800;
            long start = System.nanoTime();
            assertThat(client.validate(bearer).status()).isEqualTo(AccountValidationResult.Status.UNAVAILABLE);
            double timeoutMs = (System.nanoTime() - start) / 1_000_000d;
            backend.delayMillis = 0;
            start = System.nanoTime();
            assertThat(client.validate(bearer).status()).isEqualTo(AccountValidationResult.Status.VALID);
            double recoveryMs = (System.nanoTime() - start) / 1_000_000d;
            assertThat(backend.hits).hasValue(4);
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(Path.of("target/lot-f-recovery.json").toFile(),
                    Map.of("timeoutMs", timeoutMs, "recoveryMs", recoveryMs, "httpValidations", 4));
        }
    }

    @Test void measureFifthRequestRejectionAndRecoveryAtDefaultConcurrency() throws Exception {
        try (var backend = new Backend(); var client = backend.client()) {
            backend.entered = new CountDownLatch(4);
            backend.release = new CountDownLatch(1);
            var callers = Executors.newFixedThreadPool(4);
            var pending = new ArrayList<Future<AccountValidationResult>>();
            String bearer = AccountValidationClientTest.bearer();
            try {
                for (int i = 0; i < 4; i++) pending.add(callers.submit(() -> client.validate(bearer)));
                assertThat(backend.entered.await(800, TimeUnit.MILLISECONDS)).isTrue();
                long start = System.nanoTime();
                assertThat(client.validate(bearer).status()).isEqualTo(AccountValidationResult.Status.UNAVAILABLE);
                double rejectionMs = (System.nanoTime() - start) / 1_000_000d;
                assertThat(backend.hits).hasValue(4);
                backend.release.countDown();
                for (var request : pending) assertThat(request.get(3, TimeUnit.SECONDS).status()).isEqualTo(AccountValidationResult.Status.VALID);
                assertThat(client.validate(bearer).status()).isEqualTo(AccountValidationResult.Status.VALID);
                new ObjectMapper().writeValue(Path.of("target/lot-f-saturation.json").toFile(),
                        Map.of("fifthRequestRejectionMs", rejectionMs, "httpValidations", backend.hits.get()));
            } finally { backend.release.countDown(); callers.shutdownNow(); }
        }
    }

    private Map<String, Object> measure(int samples, Runnable operation) {
        for (int i = 0; i < 10; i++) operation.run();
        double[] values = new double[samples];
        for (int i = 0; i < samples; i++) {
            long start = System.nanoTime(); operation.run(); values[i] = (System.nanoTime() - start) / 1_000_000d;
        }
        Arrays.sort(values);
        return Map.of("samples", samples, "p50Ms", values[(int) Math.ceil(samples * .50) - 1],
                "p95Ms", values[(int) Math.ceil(samples * .95) - 1], "p99Ms", values[(int) Math.ceil(samples * .99) - 1],
                "maxMs", values[samples - 1]);
    }

    private static final class Backend implements AutoCloseable {
        final HttpServer server;
        final ExecutorService threads = Executors.newCachedThreadPool();
        final AtomicInteger hits = new AtomicInteger(), status = new AtomicInteger(204);
        volatile long delayMillis;
        volatile CountDownLatch entered = new CountDownLatch(0), release = new CountDownLatch(0);
        Backend() throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(threads);
            server.createContext("/api/internal/access-token/validate", request -> {
                hits.incrementAndGet(); entered.countDown(); int response = status.get(); long delay = delayMillis;
                try { release.await(); if (delay > 0) Thread.sleep(delay); request.sendResponseHeaders(response, -1); }
                catch (Exception ignored) { } finally { request.close(); }
            });
            server.start();
        }
        AccountValidationClient client() {
            return new AccountValidationClient(AccountValidationClientTest.JWT,
                    new AccountValidationProperties(URI.create("http://127.0.0.1:" + server.getAddress().getPort()), null, null, null));
        }
        public void close() { server.stop(0); threads.shutdownNow(); }
    }
}
