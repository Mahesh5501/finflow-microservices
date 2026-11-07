package com.finflow.apigateway.ratelimit;

import com.finflow.apigateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Configures gateway-level rate limiting using the shared Redis instance.
 * Authenticated requests are limited by user identity. Anonymous requests are limited by their network peer IP.
 * Spring Cloud Gateway combines the resolved key with the route ID, giving each route an independent rate-limit bucket.
 */
@Configuration
public class RateLimitConfig {

    /**
     * Resolves the identity used to calculate a request's rate limit.
     * A valid authenticated principal takes priority. If no usable principal exists, the network peer IP is used instead.
     */
    @Bean
    public KeyResolver rateLimitKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .filter(principal -> principal.getName() != null && !principal.getName().isBlank())
                .map(principal -> "gateway:user:" + principal.getName().trim())
                .switchIfEmpty(Mono.fromSupplier(() -> "gateway:ip:" + clientIp(exchange))); // Resolve the anonymous key only when no usable principal exists.
    }

    /**
     * Returns the network peer address.
     * We intentionally do not trust X-Forwarded-For here because clients can supply that header themselves unless a trusted proxy sanitizes it.
     */
    private static String clientIp(ServerWebExchange exchange) {
        var remoteAddress = exchange.getRequest().getRemoteAddress(); // Use the peer address, not an untrusted client-supplied forwarding header.
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    /**
     * Creates the Redis-backed token-bucket rate limiter.
     *
     * Values are supplied through app.rate-limit.* configuration:
     * replenishRate = tokens added per second,
     * burstCapacity = maximum accumulated tokens,
     * requestedTokens = tokens consumed by each request.
     */
    @Bean(name = "rateLimiter")
    public RedisRateLimiter rateLimiter(GatewayProperties properties) {
        GatewayProperties.RateLimit settings = properties.getRateLimit();
        // The framework's Redis-error policy currently allows traffic; see the review.
        return new RedisRateLimiter(settings.getReplenishRate(), settings.getBurstCapacity(), settings.getRequestedTokens());
    }
}
