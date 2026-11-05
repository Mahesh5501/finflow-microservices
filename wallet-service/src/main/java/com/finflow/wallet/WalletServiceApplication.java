package com.finflow.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Entry point for FinFlow <b>wallet-service</b> (balance authority).
 *
 * <h2>Tech stack</h2>
 * Java 21 · Spring Boot 3 · Spring Web · Spring Security (JWT/JWKS) ·
 * Spring Data JPA/Hibernate · Bean Validation · MySQL 8 · H2 (tests) · Actuator.
 *
 * <h2>Role in the platform</h2>
 * Owns wallet balances, deposits, withdrawals, P2P transfers, and history.
 * Access tokens are issued by identity-service and verified via JWKS.
 *
 * <p>Keep business logic out of this class — it only boots the Spring context.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class WalletServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }
}
