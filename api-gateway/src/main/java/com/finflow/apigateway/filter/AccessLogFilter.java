
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

import java.util.concurrent.TimeUnit;

/**
 * Logs gateway request attempts without logging request bodies or credentials.
 * Because retries can execute the downstream filter chain multiple times, this filter may produce one log entry per downstream attempt.
 */
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        long startNanos = System.nanoTime();  // nanoTime provides reliable elapsed-time measurement.

        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CorrelationIdFilter.HEADER);

        // Log once when the attempt completes, errors, or is cancelled.
        return chain.filter(exchange).doFinally(signalType -> logAttempt(exchange, correlationId, startNanos));
    }

    /**
     * Records the HTTP method, path, response status, elapsed time, and correlation ID for this downstream attempt.
     */
    private static void logAttempt(ServerWebExchange exchange, String correlationId, long startNanos) {

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
        return Ordered.LOWEST_PRECEDENCE; // Runs late in the combined gateway filter chain. Retry ordering determines whether this logs individual attempts.
    }
}
