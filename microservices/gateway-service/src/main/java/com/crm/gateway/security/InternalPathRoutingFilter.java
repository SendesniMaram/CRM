package com.crm.gateway.security;

import java.net.URI;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Rechecks rewritten request and destination paths immediately before standard routing filters. */
@Component
public final class InternalPathRoutingFilter implements GlobalFilter, Ordered {
    @Override public int getOrder() { return Ordered.LOWEST_PRECEDENCE - 10; }

    @Override public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpStatus rejection = InternalPathPolicy.rejection(exchange.getRequest().getURI().getRawPath());
        URI destination = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        if (rejection == null && destination != null) rejection = InternalPathPolicy.rejection(destination.getRawPath());
        if (rejection != null) return InternalPathWebFilter.reject(exchange, rejection);
        return chain.filter(exchange);
    }
}
