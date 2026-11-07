package com.finflow.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Logs one downstream attempt without request bodies or bearer tokens.
 * Retry can run this filter multiple times for one client request.
 */
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startMillis = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CorrelationIdFilter.HEADER);

        // Observe completion/error without blocking the reactive request chain.
        return chain.filter(exchange)
                .doOnSuccess(ignored -> logAttempt(exchange, correlationId, startMillis))
                .doOnError(error -> logAttempt(exchange, correlationId, startMillis));
    }

    private static void logAttempt(ServerWebExchange exchange, String correlationId, long startMillis) {
        Integer status = exchange.getResponse().getStatusCode() != null
                ? exchange.getResponse().getStatusCode().value()
                : null;
        log.info(
                "method={} path={} status={} durationMs={} correlationId={}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getRawPath(),
                status,
                System.currentTimeMillis() - startMillis,
                correlationId
        );
    }

    @Override
    public int getOrder() {
        // Runs inside the route retry chain; this is not a final whole-request log.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
