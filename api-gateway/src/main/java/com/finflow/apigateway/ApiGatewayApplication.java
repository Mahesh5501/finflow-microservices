package com.finflow.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts FinFlow's reactive API gateway.
 *
 * Routes and filter policies live in application.yml; business rules stay in their services.
 * Rate limiting connects to the shared Redis configured in application.properties.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
