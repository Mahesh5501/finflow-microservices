package com.finflow.apigateway.gateway.filter;

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
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";

    private static final Pattern VALID_CORRELATION_ID = Pattern.compile("^[a-zA-Z0-9._-]{1,64}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incomingId = exchange.getRequest().getHeaders().getFirst(HEADER);
        String correlationId = isValidCorrelationId(incomingId) ? incomingId.trim() : UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(HEADER, correlationId))
                .build();

        exchange.getResponse().getHeaders().set(HEADER, correlationId);
        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean isValidCorrelationId(String correlationId) {
        return correlationId != null && VALID_CORRELATION_ID.matcher(correlationId.trim()).matches();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
