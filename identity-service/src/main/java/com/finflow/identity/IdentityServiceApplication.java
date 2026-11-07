package com.finflow.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Identity service entry — owns users and JWT issue.
 *
 * <p>Stack: Java 21, Spring Boot 3, Security, JPA, MySQL, JJWT (RS256 + JWKS).
 * Wallet (18082) stays the balance authority; this service answers “who are you?”.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
