package com.finflow.apigateway.gateway.ratelimit;

import com.finflow.apigateway.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver rateLimitKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .filter(principal -> principal.getName() != null && !principal.getName().isBlank())
                .map(principal -> "gateway:user:" + principal.getName().trim())
                .switchIfEmpty(Mono.fromSupplier(() -> "gateway:ip:" + clientIp(exchange)));
    }

    private static String clientIp(ServerWebExchange exchange) {
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    @Bean(name = "rateLimiter")
    public RedisRateLimiter rateLimiter(GatewayProperties properties) {
        GatewayProperties.RateLimit settings = properties.getRateLimit();
        return new RedisRateLimiter(settings.getReplenishRate(), settings.getBurstCapacity(), settings.getRequestedTokens());
    }
}
