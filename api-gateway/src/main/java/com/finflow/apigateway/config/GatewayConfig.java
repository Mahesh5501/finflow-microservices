package com.finflow.apigateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Configures Cross-Origin Resource Sharing (CORS) for the API Gateway.
 * Browser clients use CORS when calling the gateway from a different origin. Settings are loaded from the app.cors.* properties.
 */
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayConfig {

    /**
     * Creates a global reactive CORS filter.
     * Applying the policy to /** ensures that browser preflight requests and actual API requests use the same configured CORS rules.
     */
    @Bean
    public CorsWebFilter corsWebFilter(GatewayProperties properties) {
        GatewayProperties.Cors settings = properties.getCors();
        CorsConfiguration cors = new CorsConfiguration();

        cors.setAllowedOriginPatterns(settings.getAllowedOrigins()); // Configure which browser origins may call this gateway.
        cors.setAllowedMethods(settings.getAllowedMethods()); // Configure which HTTP methods are permitted.
        cors.setAllowedHeaders(settings.getAllowedHeaders()); // Configure which request headers are permitted.
        cors.setAllowCredentials(false); // Credentials are disabled: browsers cannot use this CORS policy to make credentialed cross-origin requests.
        cors.setMaxAge(settings.getMaxAgeSeconds());  // Controls how long browsers may cache preflight responses.

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return new CorsWebFilter(source);
    }
}
