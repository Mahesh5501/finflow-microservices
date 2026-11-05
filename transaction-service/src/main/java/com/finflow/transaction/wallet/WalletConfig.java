package com.finflow.transaction.wallet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WalletConfig {

    @Bean
    @ConditionalOnProperty(name = "wallet.provider", havingValue = "wallet")
    RestClient walletHttpClient(@Value("${wallet.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
