package com.finflow.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FinFlow <b>api-gateway</b> — production-style edge.
 *
 * <p>Rate limiting uses shared Redis ({@code localhost:6379} via infrastructure docker-compose).
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
