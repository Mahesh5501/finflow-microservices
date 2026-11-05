package com.finflow.apigateway.ratelimit;

import com.finflow.apigateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Edge rate limiting (token bucket) via shared Docker Redis.
 * Keys: {@code gateway:user:} / {@code gateway:ip:} so identity’s keyspace stays separate.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver rateLimitKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(p -> "gateway:user:" + p.getName())
                .switchIfEmpty(Mono.fromCallable(() -> {
                    var remote = exchange.getRequest().getRemoteAddress();
                    String ip = remote != null && remote.getAddress() != null
                            ? remote.getAddress().getHostAddress()
                            : "unknown";
                    return "gateway:ip:" + ip;
                }));
    }

    @Bean(name = "rateLimiter")
    public RedisRateLimiter rateLimiter(GatewayProperties properties) {
        GatewayProperties.RateLimit rl = properties.getRateLimit();
        return new RedisRateLimiter(rl.getReplenishRate(), rl.getBurstCapacity(), rl.getRequestedTokens());
    }
}
