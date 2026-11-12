package com.finflow.apigateway.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Configures Cross-Origin Resource Sharing (CORS) for the API Gateway.
 */
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayConfig {

    @Bean
    public CorsWebFilter corsWebFilter(GatewayProperties properties) {
        GatewayProperties.Cors settings = properties.getCors();
        CorsConfiguration cors = new CorsConfiguration();

        cors.setAllowedOriginPatterns(settings.getAllowedOrigins());
        cors.setAllowedMethods(settings.getAllowedMethods());
        cors.setAllowedHeaders(settings.getAllowedHeaders());
        cors.setAllowCredentials(false);
        cors.setMaxAge(settings.getMaxAgeSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return new CorsWebFilter(source);
    }
}
