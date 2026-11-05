package com.finflow.commerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Commerce owns catalogue, cart, stock, and orders — not wallet balances.
 * Payment goes through PaymentPort (wallet HTTP or simulator) after outbox commit.
 */
@SpringBootApplication
@EnableScheduling
public class CommerceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommerceServiceApplication.class, args);
    }
}
