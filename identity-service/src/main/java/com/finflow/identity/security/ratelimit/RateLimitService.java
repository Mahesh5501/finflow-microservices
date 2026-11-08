package com.finflow.identity.security.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fixed-window rate limiter for abuse-prone authentication endpoints using shared Redis.
 *
 * Keys: identity:ratelimit:...
 * Counter increment and expiration are performed atomically by Redis.
 */
@Service
public class RateLimitService {

    /**
     * Increments the counter and sets its expiration on the first request.
     * Both operations execute atomically inside Redis.
     */
    private static final DefaultRedisScript<Long> INCREMENT_AND_EXPIRE_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local current = redis.call('INCR', KEYS[1])

                    if current == 1 then
                        redis.call('PEXPIRE', KEYS[1], ARGV[1])
                    end

                    return current
                    """,
                    Long.class
            );

    private final int maxAttempts;
    private final long windowMs;
    private final StringRedisTemplate redis;

    public RateLimitService(
            StringRedisTemplate redis,
            @Value("${rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${rate-limit.window-ms:600000}") long windowMs
    ) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("rate-limit.max-attempts must be greater than zero");
        }

        if (windowMs <= 0) {
            throw new IllegalArgumentException("rate-limit.window-ms must be greater than zero");
        }

        this.redis = redis;
        this.maxAttempts = maxAttempts;
        this.windowMs = windowMs;
    }

    /**
     * @return true if the request is allowed; false if the limit is exceeded
     * @throws org.springframework.dao.DataAccessException when Redis is unreachable (caller must fail closed with a clear 503 — never treat this as missing Bearer auth)
     */
    public boolean tryConsume(String key) {
        String redisKey = "identity:ratelimit:" + key;
        Long count = redis.execute(INCREMENT_AND_EXPIRE_SCRIPT, List.of(redisKey), String.valueOf(windowMs));
        return count != null && count <= maxAttempts;
    }
}
