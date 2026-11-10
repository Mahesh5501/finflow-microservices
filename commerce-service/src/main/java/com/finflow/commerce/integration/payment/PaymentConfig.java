package com.finflow.commerce.integration.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration for payment-related infrastructure.
 * This class currently configures the RestClient used to communicate with Wallet Service.
 * It creates the RestClient only when payment.provider=wallet.
 */
@Configuration
public class PaymentConfig {

    /**
     * Creates a reusable RestClient for Wallet Service.
     * @Bean tells Spring to register the returned object in the application context so other Spring-managed classes can inject it.
     * @ConditionalOnProperty prevents creating this client when the simulated payment implementation is active.
     * @param baseUrl the base URL of the Wallet Service.
     * @return the RestClient for Wallet Service.
     */
    @Bean
    @ConditionalOnProperty(
            name = "payment.provider",
            havingValue = "wallet"
    )
    RestClient walletRestClient(
            @Value("${wallet.base-url}") String baseUrl
    ) {
        // Configure the base URL once. Adapter methods can then use relative paths such as: /api/v1/users/{userId}/wallet This avoids hardcoding the host in every HTTP call.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}