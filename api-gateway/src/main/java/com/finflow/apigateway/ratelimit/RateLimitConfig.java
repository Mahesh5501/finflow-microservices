package com.finflow.apigateway.ratelimit;

import com.finflow.apigateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Edge rate limiting (token bucket) via shared Docker Redis.
 * Keys use the authenticated subject, or the socket IP for an anonymous caller.
 * Spring adds the route ID to the Redis key, so each route has its own budget.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver rateLimitKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(principal -> "gateway:user:" + principal.getName())
                // Evaluate the anonymous fallback only when there is no principal.
                .switchIfEmpty(Mono.fromSupplier(() -> "gateway:ip:" + clientIp(exchange)));
    }

    private static String clientIp(ServerWebExchange exchange) {
        // Use the peer address, not an untrusted client-supplied forwarding header.
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    @Bean(name = "rateLimiter")
    public RedisRateLimiter rateLimiter(GatewayProperties properties) {
        GatewayProperties.RateLimit settings = properties.getRateLimit();
        // The framework's Redis-error policy currently allows traffic; see the review.
        return new RedisRateLimiter(
                settings.getReplenishRate(), settings.getBurstCapacity(), settings.getRequestedTokens()
        );
    }
}
