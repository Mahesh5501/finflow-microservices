package com.finflow.apigateway.gateway.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GlobalErrorConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GlobalErrorConfig handler = new GlobalErrorConfig(objectMapper);

    @ParameterizedTest
    @CsvSource({
            "NOT_FOUND, ROUTE_NOT_FOUND, No gateway route matched",
            "TOO_MANY_REQUESTS, RATE_LIMITED, Too Many Requests",
            "PAYLOAD_TOO_LARGE, PAYLOAD_TOO_LARGE, Payload Too Large",
            "BAD_REQUEST, BAD_REQUEST, Bad Request",
            "INTERNAL_SERVER_ERROR, GATEWAY_ERROR, Internal Server Error"
    })
    void preservesHttpStatusAndErrorBody(HttpStatus status, String code, String message) throws Exception {
        MockServerWebExchange exchange = exchange();

        handler.handle(exchange, new ResponseStatusException(status)).block();

        JsonNode body = responseBody(exchange);
        assertEquals(status, exchange.getResponse().getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, exchange.getResponse().getHeaders().getContentType());
        assertEquals(code, body.get("code").asText());
        assertEquals(message, body.get("message").asText());
        assertEquals(status.value(), body.get("status").asInt());
        assertFalse(body.has("correlationId"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void mapsDirectAndWrappedTimeouts(boolean wrapped) throws Exception {
        MockServerWebExchange exchange = exchange();
        Throwable error = new TimeoutException("backend timed out");
        if (wrapped) {
            error = new RuntimeException(error);
        }

        handler.handle(exchange, error).block();

        JsonNode body = responseBody(exchange);
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, exchange.getResponse().getStatusCode());
        assertEquals("UPSTREAM_TIMEOUT", body.get("code").asText());
        assertEquals("Upstream service timed out", body.get("message").asText());
    }

    @Test
    void preservesExplicitStatusReason() throws Exception {
        MockServerWebExchange exchange = exchange();

        handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND, "Missing route")).block();

        assertEquals("Missing route", responseBody(exchange).get("message").asText());
    }

    @Test
    void includesExistingCorrelationId() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/missing").header("X-Correlation-Id", "request-123")
        );

        handler.handle(exchange, new IllegalStateException("unexpected")).block();

        JsonNode body = responseBody(exchange);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        assertEquals("request-123", body.get("correlationId").asText());
        assertEquals("GATEWAY_ERROR", body.get("code").asText());
    }

    @Test
    void doesNotOverwriteCommittedResponse() {
        MockServerWebExchange exchange = exchange();
        exchange.getResponse().setStatusCode(HttpStatus.ACCEPTED);
        exchange.getResponse().setComplete().block();
        RuntimeException original = new IllegalStateException("too late");

        RuntimeException propagated = assertThrows(
                RuntimeException.class, () -> handler.handle(exchange, original).block()
        );

        assertSame(original, propagated);
        assertEquals(HttpStatus.ACCEPTED, exchange.getResponse().getStatusCode());
    }

    @Test
    void returnsFallbackJsonWhenSerializationFails() throws Exception {
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public byte[] writeValueAsBytes(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("cannot serialize") { };
            }
        };
        MockServerWebExchange exchange = exchange();

        new GlobalErrorConfig(failingMapper)
                .handle(exchange, new ResponseStatusException(HttpStatus.BAD_REQUEST)).block();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        assertEquals("Unexpected error", responseBody(exchange).get("message").asText());
        assertEquals("GATEWAY_ERROR", responseBody(exchange).get("code").asText());
    }

    private static MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/missing"));
    }

    private JsonNode responseBody(MockServerWebExchange exchange) throws Exception {
        return objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
    }
}
