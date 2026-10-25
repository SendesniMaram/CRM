package com.crm.security;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Clock;
import java.util.concurrent.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import io.jsonwebtoken.JwtException;

/** Fixed destination, no redirects, positive cache or application retries. */
public class AccountValidationClient implements AutoCloseable {
    private final JwtService jwt;
    private final AccountValidationProperties properties;
    private final Exchange exchange;
    private final Clock clock;
    // A stuck DNS lookup cannot create unlimited threads or an unbounded queue.
    private final ExecutorService workers = new ThreadPoolExecutor(0, 4, 30, TimeUnit.SECONDS,
            new SynchronousQueue<>(), task -> {
                Thread thread = new Thread(task, "account-validation");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    public AccountValidationClient(JwtService jwt, AccountValidationProperties properties) {
        this(jwt, properties, httpExchange(properties), Clock.systemUTC());
    }

    AccountValidationClient(JwtService jwt, AccountValidationProperties properties, Exchange exchange, Clock clock) {
        this.jwt = java.util.Objects.requireNonNull(jwt);
        this.properties = java.util.Objects.requireNonNull(properties);
        this.exchange = java.util.Objects.requireNonNull(exchange);
        this.clock = java.util.Objects.requireNonNull(clock);
    }

    private static Exchange httpExchange(AccountValidationProperties properties) {
        var factory = new SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                connection.setInstanceFollowRedirects(false);
                connection.setUseCaches(false);
            }
        };
        factory.setProxy(java.net.Proxy.NO_PROXY);
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        RestClient client = RestClient.builder().requestFactory(factory).build();
        var endpoint = properties.baseUrl().resolve("/api/internal/access-token/validate");
        return authorization -> client.get().uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .exchange((request, response) -> response.getStatusCode().value());
    }

    public AccountValidationResult validate(String authorization) {
        JwtService.AccessTokenClaims claims;
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")
                    || authorization.indexOf('\r') >= 0 || authorization.indexOf('\n') >= 0) {
                return AccountValidationResult.invalid();
            }
            claims = jwt.parseAccessToken(authorization.substring(7));
        } catch (JwtException | IllegalArgumentException ex) {
            return AccountValidationResult.invalid();
        }
        Future<Integer> pending = null;
        try {
            pending = workers.submit(() -> exchange.status(authorization));
            int status = pending.get(properties.totalTimeout().toNanos(), TimeUnit.NANOSECONDS);
            if (status == 401) return AccountValidationResult.invalid();
            if (status != 204) return AccountValidationResult.unavailable();
            if (!claims.expiresAt().isAfter(clock.instant())) return AccountValidationResult.invalid();
            return new AccountValidationResult(AccountValidationResult.Status.VALID, claims);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return AccountValidationResult.unavailable();
        } catch (ExecutionException | TimeoutException | RejectedExecutionException ex) {
            return AccountValidationResult.unavailable();
        } finally {
            if (pending != null && !pending.isDone()) pending.cancel(true);
        }
    }

    @Override public void close() { workers.shutdownNow(); }
    @FunctionalInterface interface Exchange { int status(String authorization) throws Exception; }
}
