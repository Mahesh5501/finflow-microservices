package com.finflow.commerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for Commerce Service.
 *
 * Commerce owns catalogue, cart, stock and orders.
 * It does not own wallet balances.
 *
 * Payments are handled through PaymentGateway:
 * - Wallet HTTP integration
 * - Simulated payment implementation
 *
 * @SpringBootApplication combines configuration, component scanning and auto
 * configuration. Its root com.finflow.commerce includes all feature packages; Boot's
 * default entity/repository scanning covers their domain/persistence packages too.
 * Moving classes below this root requires no explicit @EntityScan or repository list.
 * Moving the entry point lower would require explicit scan roots. Integration tests
 * load this context to catch missing gateways, services and repository discovery.
 * No scheduler is enabled here; maintenance classes remain callable use cases.
 */
@SpringBootApplication
public class CommerceServiceApplication {

    /**
     * Java application entry point.
     *
     * Starts the Spring Boot application and its application context.
     */
    public static void main(String[] args) {

        SpringApplication.run(
                CommerceServiceApplication.class,
                args
        );
    }
}
