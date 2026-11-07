package com.finflow.apigateway.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Typed settings bound from app.* properties.
 * Routing/filter configuration remains in application.yml.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public class GatewayProperties {

    // Nested validation is not yet cascaded; adding @Valid is a separate reviewed fix.
    private final RateLimit rateLimit = new RateLimit();
    private final Http http = new Http();
    private final Cors cors = new Cors();

    // Currently not wired to RequestSize: YAML still declares a separate 1MB limit.
    @Min(1024)
    private long maxBodyBytes = 1_048_576;

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Http getHttp() {
        return http;
    }

    public Cors getCors() {
        return cors;
    }

    public long getMaxBodyBytes() {
        return maxBodyBytes;
    }

    public void setMaxBodyBytes(long maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    public static class RateLimit {
        // Tokens restored each second, maximum saved tokens, and cost per request.
        @Min(1)
        private int replenishRate = 50;

        @Min(1)
        private int burstCapacity = 100;

        @Min(1)
        private int requestedTokens = 1;

        public int getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public int getRequestedTokens() {
            return requestedTokens;
        }

        public void setRequestedTokens(int requestedTokens) {
            this.requestedTokens = requestedTokens;
        }
    }

    public static class Http {
        // Downstream connection and response deadlines, in milliseconds.
        @Min(100)
        private int connectTimeoutMs = 2000;

        @Min(100)
        private int responseTimeoutMs = 5000;

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getResponseTimeoutMs() {
            return responseTimeoutMs;
        }

        public void setResponseTimeoutMs(int responseTimeoutMs) {
            this.responseTimeoutMs = responseTimeoutMs;
        }
    }

    public static class Cors {
        // Local defaults allow any origin. Production origins need an explicit policy.
        @NotEmpty
        private List<String> allowedOrigins = List.of("*");

        @NotEmpty
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

        @NotEmpty
        private List<String> allowedHeaders = List.of("*");

        @Min(0)
        private long maxAgeSeconds = 3600;

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public long getMaxAgeSeconds() {
            return maxAgeSeconds;
        }

        public void setMaxAgeSeconds(long maxAgeSeconds) {
            this.maxAgeSeconds = maxAgeSeconds;
        }
    }
}
