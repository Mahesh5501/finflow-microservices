package com.finflow.apigateway.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startNanos = System.nanoTime();
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CorrelationIdFilter.HEADER);
        return chain.filter(exchange).doFinally(signalType -> logAttempt(exchange, correlationId, startNanos));
    }

    private void logAttempt(ServerWebExchange exchange, String correlationId, long startNanos) {
        Integer status = exchange.getResponse().getStatusCode() != null ? exchange.getResponse().getStatusCode().value() : null;
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        log.info(
                "method={} path={} status={} durationMs={} correlationId={}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getRawPath(),
                status,
                durationMs,
                correlationId);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
