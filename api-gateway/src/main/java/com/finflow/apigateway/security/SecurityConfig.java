package com.finflow.apigateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Authenticates protected API requests using identity-service's JWKS.
 * Business services still enforce permissions, organisation access and ownership.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                // This API uses bearer headers rather than form login or HTTP Basic.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // Browser CORS preflight requests are public.
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll() // Health endpoints remain publicly accessible.
                        .pathMatchers("/fallback/**").permitAll() // Gateway fallback endpoints remain public.
                        .pathMatchers(HttpMethod.GET, "/.well-known/jwks.json").permitAll() // Public JWKS endpoint.
                        .pathMatchers(HttpMethod.POST, // Public authentication and onboarding endpoints.
                                "/api/v1/auth/register",
                                "/api/v1/auth/verify-email",
                                "/api/v1/auth/resend-verification",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password"
                        ).permitAll()
                        .pathMatchers("/api/v1/**").authenticated()  // All other API endpoints require authentication.
                        .pathMatchers("/actuator/**").denyAll() // Other actuator endpoints are not publicly accessible.
                        .anyExchange().denyAll() // Deny every route not explicitly covered above.
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()) // Signature/timestamp verification is delegated to Spring Security.
                        .authenticationEntryPoint(authenticationFailureHandler()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationFailureHandler())
                        .accessDeniedHandler(accessDeniedHandler()));
        return http.build();
    }

    /** 401: authentication is missing or the access token was rejected. */
    private ServerAuthenticationEntryPoint authenticationFailureHandler() {
        return (exchange, ex) -> writeJson(
                exchange.getResponse(),
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Valid Bearer access token required");
    }

    /** 403: an authenticated request is not permitted by the security rules. */
    private ServerAccessDeniedHandler accessDeniedHandler() {
        return (exchange, denied) -> writeJson(
                exchange.getResponse(),
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Access denied");
    }

    private Mono<Void> writeJson(ServerHttpResponse response, HttpStatus status, String code, String message) {
        if (response.isCommitted()) {
            return Mono.empty();
        }
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("status", status.value());
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            byte[] bytes = ("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        }
    }
}
