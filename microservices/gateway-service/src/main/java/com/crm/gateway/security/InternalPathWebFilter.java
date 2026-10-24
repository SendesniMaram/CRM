package com.crm.gateway.security;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Runs before route selection, including discovery-generated routes and CORS handlers. */
@Component
public final class InternalPathWebFilter implements WebFilter, Ordered {
    @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }

    @Override public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpStatus rejection = InternalPathPolicy.rejection(exchange.getRequest().getURI().getRawPath());
        if (rejection != null) return reject(exchange, rejection);
        return chain.filter(exchange);
    }

    static Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        return exchange.getResponse().setComplete();
    }
}
