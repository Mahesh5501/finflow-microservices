package com.finflow.commerce.health;

import com.finflow.commerce.security.CommerceSecurityProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reports commerce-specific dependency readiness (database, cache, identity JWKS, wallet).
 * Intentionally excludes JVM heap, GC, and thread metrics (those stay on Actuator /metrics).
 */
@Service
public class CommerceDependencyHealthService {

    private record Check(String name, String status, String detail) {
        static Check up(String name) {
            return new Check(name, "UP", null);
        }

        static Check down(String name, String detail) {
            return new Check(name, "DOWN", detail);
        }

        static Check skipped(String name, String detail) {
            return new Check(name, "SKIPPED", detail);
        }
    }

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final RestClient probeClient;
    private final String jwksUrl;
    private final String paymentProvider;
    private final ObjectProvider<RestClient> walletRestClient;
    private final CommerceSecurityProperties securityProperties;

    public CommerceDependencyHealthService(
            DataSource dataSource,
            RedisConnectionFactory redisConnectionFactory,
            @Value("${identity.jwks-url}") String jwksUrl,
            @Value("${payment.provider}") String paymentProvider,
            @Value("${wallet.base-url:http://localhost:18082}") String walletBaseUrl,
            ObjectProvider<RestClient> walletRestClient,
            CommerceSecurityProperties securityProperties
    ) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
        this.jwksUrl = jwksUrl;
        this.paymentProvider = paymentProvider;
        this.walletRestClient = walletRestClient;
        this.securityProperties = securityProperties;
        this.probeClient = RestClient.builder().baseUrl(walletBaseUrl).build();
    }

    public Map<String, Object> snapshot() {
        List<Check> checks = new ArrayList<>();
        checks.add(probeDatabase());
        checks.add(probeRedis());
        checks.add(probeJwks());
        checks.add(probeWallet());

        String aggregate = checks.stream().anyMatch(c -> "DOWN".equals(c.status())) ? "DOWN" : "UP";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", aggregate);
        body.put("service", "commerce-service");
        body.put("checks", checks);
        return body;
    }

    private Check probeDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2)
                    ? Check.up("database")
                    : Check.down("database", "connection invalid");
        } catch (Exception ex) {
            return Check.down("database", ex.getClass().getSimpleName());
        }
    }

    private Check probeRedis() {
        try {
            String pong = redisConnectionFactory.getConnection().ping();
            return "PONG".equalsIgnoreCase(pong)
                    ? Check.up("redis")
                    : Check.down("redis", "unexpected ping response");
        } catch (Exception ex) {
            return Check.down("redis", ex.getClass().getSimpleName());
        }
    }

    private Check probeJwks() {
        if (!securityProperties.jwtValidationEnabled()) {
            return Check.skipped("identity-jwks", "JWT validation disabled for local testing");
        }
        try {
            RestClient.create().get().uri(jwksUrl).retrieve().toBodilessEntity();
            return Check.up("identity-jwks");
        } catch (Exception ex) {
            return Check.down("identity-jwks", ex.getClass().getSimpleName());
        }
    }

    private Check probeWallet() {
        if (!"wallet".equalsIgnoreCase(paymentProvider)) {
            return Check.skipped("wallet", "payment.provider!=wallet");
        }
        RestClient client = walletRestClient.getIfAvailable();
        if (client == null) {
            client = probeClient;
        }
        try {
            client.get().uri("/actuator/health").retrieve().toBodilessEntity();
            return Check.up("wallet");
        } catch (Exception ex) {
            return Check.down("wallet", ex.getClass().getSimpleName());
        }
    }
}
