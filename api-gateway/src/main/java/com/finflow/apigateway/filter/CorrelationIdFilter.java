package com.finflow.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Adds or propagates X-Correlation-Id for requests handled by gateway routes.
 * A correlation ID allows us to trace a request across multiple services. The same ID is forwarded to downstream services and returned to the client.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";

    // Accept only simple, trace-friendly IDs to prevent malformed or excessively long client-supplied values from entering application logs.
    private static final Pattern VALID_CORRELATION_ID = Pattern.compile("^[a-zA-Z0-9._-]{1,64}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String incomingId = exchange.getRequest().getHeaders().getFirst(HEADER); // Read the correlation ID supplied by the incoming client request.

        // Reuse the incoming ID only when it matches our accepted format. Otherwise, generate a new UUID so every request has a valid ID.
        String correlationId = isValidCorrelationId(incomingId) ? incomingId.trim() : UUID.randomUUID().toString();

        // ServerHttpRequest is immutable, so create a modified request containing the correlation ID before forwarding it downstream.
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(HEADER, correlationId))
                .build();

        exchange.getResponse().getHeaders().set(HEADER, correlationId); // Return the same ID to the client for debugging and issue tracking.
        return chain.filter(exchange.mutate().request(request).build());
    }

    /**
     * Validates the incoming correlation ID.
     * Allows 1-64 characters containing only letters, digits, dots, underscores, and hyphens.
     */
    private boolean isValidCorrelationId(String correlationId) {
        return correlationId != null && VALID_CORRELATION_ID.matcher(correlationId.trim()).matches();
    }

    @Override
    public int getOrder() {
        // Runs first within the gateway route filter chain. This does not mean it runs before Spring Security.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
