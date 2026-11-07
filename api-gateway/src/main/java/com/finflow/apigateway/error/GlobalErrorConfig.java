
package com.finflow.apigateway.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Converts unhandled API Gateway exceptions into a consistent JSON response.
 *
 * This handler covers exceptions reaching the gateway's global error handling.
 * Downstream responses and filters that have already written their own response are not rewritten here.
 */
@Component
@Order(-2)
public class GlobalErrorConfig implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorConfig.class);

    private final ObjectMapper objectMapper;

    public GlobalErrorConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = resolveStatus(ex);
        String code = resolveCode(status);
        String message = resolveMessage(ex, status);

        if (status.is5xxServerError()) {
            log.error("Gateway error status={} path={} msg={}", status.value(), exchange.getRequest().getPath(), ex.toString());
        } else {
            log.warn("Gateway client error status={} path={} msg={}", status.value(), exchange.getRequest().getPath(), ex.toString());
        }

        ServerHttpResponse response = exchange.getResponse();

        // A committed response cannot safely be replaced with a new error body.
        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("status", status.value());
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        if (correlationId != null) {
            body.put("correlationId", correlationId);
        }

        return writeJson(response, body);
    }

    /**
     * Serializes the error response without blocking the reactive pipeline.
     * A small static JSON response is used if serialization itself fails.
     */
    private Mono<Void> writeJson(ServerHttpResponse response, Map<String, Object> body) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            // Keep the response writable even if JSON serialization fails.
            byte[] bytes = "{\"code\":\"GATEWAY_ERROR\",\"message\":\"Unexpected error\"}"
                    .getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        }
    }

    /**
     * Maps known exception types to HTTP status codes.
     * Unknown or unsupported status codes safely become HTTP 500.
     */
    private static HttpStatus resolveStatus(Throwable ex) {

        if (ex instanceof ResponseStatusException rse) {
            HttpStatus resolved = HttpStatus.resolve(rse.getStatusCode().value());

            return resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // A timeout may be wrapped by one or more reactive exceptions.
        Throwable current = ex;

        while (current != null) {
            if (current instanceof TimeoutException) {
                return HttpStatus.GATEWAY_TIMEOUT;
            }

            current = current.getCause();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String resolveCode(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "ROUTE_NOT_FOUND";
            case GATEWAY_TIMEOUT -> "UPSTREAM_TIMEOUT";
            case TOO_MANY_REQUESTS -> "RATE_LIMITED";
            case PAYLOAD_TOO_LARGE -> "PAYLOAD_TOO_LARGE";
            default -> status.is4xxClientError() ? "BAD_REQUEST" : "GATEWAY_ERROR";
        };
    }

    private static String resolveMessage(Throwable ex, HttpStatus status) {
        if (ex instanceof ResponseStatusException rse && rse.getReason() != null) {
            return rse.getReason();
        }
        if (status == HttpStatus.GATEWAY_TIMEOUT) {
            return "Upstream service timed out";
        }
        if (status == HttpStatus.NOT_FOUND) {
            return "No gateway route matched";
        }
        return status.getReasonPhrase();
    }
}
