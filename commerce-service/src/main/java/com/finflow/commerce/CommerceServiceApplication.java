
package com.finflow.commerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for Commerce Service.
 *
 * Commerce owns catalogue, cart, stock and orders.
 * It does not own wallet balances.
 *
 * Payments are handled through PaymentPort:
 * - Wallet HTTP integration
 * - Simulated payment implementation
 */
@SpringBootApplication
@EnableScheduling
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
