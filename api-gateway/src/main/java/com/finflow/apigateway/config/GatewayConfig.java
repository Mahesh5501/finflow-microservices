package com.finflow.apigateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/** Builds the browser CORS policy from app.cors.* settings. */
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
        // Do not opt into credentialed cross-origin browser requests.
        cors.setAllowCredentials(false);
        cors.setMaxAge(settings.getMaxAgeSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return new CorsWebFilter(source);
    }
}
