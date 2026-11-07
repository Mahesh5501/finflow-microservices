package com.finflow.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adds or propagates X-Correlation-Id for matched gateway routes.
 * Security rejections and local endpoints can skip this route filter.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incomingId = exchange.getRequest().getHeaders().getFirst(HEADER);
        String correlationId = (incomingId == null || incomingId.isBlank())
                ? UUID.randomUUID().toString()
                : incomingId.trim();

        // Spring requests are immutable: forward a copy containing the ID.
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(HEADER, correlationId)
                .build();

        // Echo the same ID so the client can include it in an issue report.
        exchange.getResponse().getHeaders().set(HEADER, correlationId);
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        // First within the gateway route chain, not before Spring Security.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
