
package com.finflow.apigateway.gateway.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalErrorConfig implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

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

    private Mono<Void> writeJson(ServerHttpResponse response, Map<String, Object> body) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            byte[] bytes = "{\"code\":\"GATEWAY_ERROR\",\"message\":\"Unexpected error\"}"
                    .getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        }
    }

    private static HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            HttpStatus resolved = HttpStatus.resolve(rse.getStatusCode().value());
            return resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
        }
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
