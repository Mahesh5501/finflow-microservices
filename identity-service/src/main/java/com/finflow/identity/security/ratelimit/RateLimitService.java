package com.finflow.identity.security.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Fixed-window rate limiter for abuse-prone auth endpoints via shared Docker Redis.
 * Keys: {@code identity:ratelimit:…} — see infrastructure/README.md.
 */
@Service
public class RateLimitService {

    private final int maxAttempts;
    private final long windowMs;
    private final StringRedisTemplate redis;

    public RateLimitService(
            StringRedisTemplate redis,
            @Value("${rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${rate-limit.window-ms:600000}") long windowMs
    ) {
        this.redis = redis;
        this.maxAttempts = maxAttempts;
        this.windowMs = windowMs;
    }

    /** @return true if allowed, false if over limit */
    public boolean tryConsume(String key) {
        String redisKey = "identity:ratelimit:" + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redis.expire(redisKey, Duration.ofMillis(windowMs));
        }
        return count != null && count <= maxAttempts;
    }
}
