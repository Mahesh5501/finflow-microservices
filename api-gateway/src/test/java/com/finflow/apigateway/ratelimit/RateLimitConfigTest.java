package com.finflow.apigateway.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitConfigTest {

    private final KeyResolver resolver = new RateLimitConfig().rateLimitKeyResolver();

    @Test
    void usesAuthenticatedSubjectInsteadOfIp() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/wallets/1"))
                .mutate()
                .principal(Mono.just(UsernamePasswordAuthenticationToken.authenticated("42", null, List.of())))
                .build();

        assertEquals("gateway:user:42", resolver.resolve(exchange).block());
    }

    @Test
    void usesPeerIpForAnonymousCallerRatherThanForwardedHeader() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/login")
                        .remoteAddress(new InetSocketAddress("127.0.0.1", 1234))
                        .header("X-Forwarded-For", "203.0.113.99")
        );

        assertEquals("gateway:ip:127.0.0.1", resolver.resolve(exchange).block());
    }

    @Test
    void usesUnknownWhenPeerAddressIsMissing() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/auth/login"));

        assertEquals("gateway:ip:unknown", resolver.resolve(exchange).block());
    }
}
