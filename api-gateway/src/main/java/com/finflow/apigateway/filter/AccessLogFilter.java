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
 * Structured access log (no bodies — avoid leaking tokens/PII).
 * Interview: gateway is the right place for edge request metrics/logs.
 */
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String correlation = request.getHeaders().getFirst(CorrelationIdFilter.HEADER);

        return chain.filter(exchange)
                .doOnSuccess(v -> logLine(exchange, correlation, start))
                .doOnError(err -> logLine(exchange, correlation, start));
    }

    private static void logLine(ServerWebExchange exchange, String correlation, long start) {
        Integer status = exchange.getResponse().getStatusCode() != null
                ? exchange.getResponse().getStatusCode().value()
                : null;
        log.info(
                "method={} path={} status={} durationMs={} correlationId={}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getRawPath(),
                status,
                System.currentTimeMillis() - start,
                correlation
        );
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
