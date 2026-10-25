package com.crm.gateway.security;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class InternalPathPolicyTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "/api/internal/access-token/validate", "/identity/api/internal/access-token/validate",
            "/identity-service/api/internal/access-token/validate", "/IDENTITY-SERVICE/API/INTERNAL/access-token/validate",
            "/arbitrary/prefix/api/internal/access-token/validate", "/api/internal", "/api/internal/another-endpoint",
            "/identity-service//api///internal/access-token/validate", "/api/./internal/access-token/validate",
            "/api/private/../internal/access-token/validate", "/api/internal/../auth/login",
            "/identity/api/auth/../internal/access-token/validate",
            "/api;v=1/internal;x=2/access-token/validate", "/api/%69nternal/access-token/validate",
            "/%61pi/internal/access-token/validate", "/api%2finternal/access-token/validate",
            "/identity-service%2Fapi%2Finternal%2Faccess-token%2Fvalidate",
            "/api%252finternal/access-token/validate", "/api%25252finternal/access-token/validate",
            "/api/%2e/internal/access-token/validate", "/api/private/%2e%2e/internal/access-token/validate",
            "/api%5cinternal/access-token/validate", "/api\\internal/access-token/validate",
            "/api%3bv=1/internal/access-token/validate"
    })
    void internalNamespaceIsNeverPublic(String path) {
        assertThat(InternalPathPolicy.rejection(path)).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/%ZZ", "/api/%", "/api/%25ZZ", "/api/%00/users", "/api/%0d%0a/users",
            "/api/%3f/users", "/api/%23/users", "/api/%25252525252525252525252f/users",
            "/api%c0%afinternal/access-token/validate", "/api%e0%80%afinternal/access-token/validate", "/api/%ff/users"})
    void ambiguousOrMalformedPathsAreRejected(String path) {
        assertThat(InternalPathPolicy.rejection(path)).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/identity/api/auth/login", "/identity/api/auth/refresh", "/identity/api/users",
            "/identity-service/api/auth/login", "/employee/api/employees/1", "/actuator/health",
            "/api/internalized", "/api/users/internal", "/api/users/Jos%C3%A9"})
    void legitimatePathsRemainUnchanged(String path) {
        assertThat(InternalPathPolicy.rejection(path)).isNull();
    }

    @Test void ingressRejectsBeforeAnyDownstreamHandler() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/identity-service/api/internal/access-token/validate"));
        new InternalPathWebFilter().filter(exchange, e -> { throw new AssertionError("handler executed"); }).block(Duration.ofSeconds(1));
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    @Test void finalDestinationIsCheckedEvenWhenRequestPathLooksPublic() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/public-alias"));
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR,
                URI.create("http://identity-service:8082/api/internal/access-token/validate"));
        new InternalPathRoutingFilter().filter(exchange, e -> { throw new AssertionError("routed"); }).block(Duration.ofSeconds(1));
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
