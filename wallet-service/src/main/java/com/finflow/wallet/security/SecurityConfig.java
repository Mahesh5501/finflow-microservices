package com.finflow.wallet.security;

import com.finflow.wallet.security.WalletProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Wallet APIs require a valid identity JWT + permission claims.
 *
 * <pre>
 * Permission strings come from {@code wallet.security.read-permission} and
 * {@code wallet.security.write-permission} in configuration.
 * </pre>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final WalletProperties walletProperties;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, WalletProperties walletProperties) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.walletProperties = walletProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String readPermission = walletProperties.getSecurity().getReadPermission();
        String writePermission = walletProperties.getSecurity().getWritePermission();
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/customer/wallet/**",
                                "/api/v1/wallets/**",
                                "/api/v1/users/**"
                        ).hasAuthority(readPermission)
                        .requestMatchers(HttpMethod.POST, "/api/v1/**").hasAuthority(writePermission)
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"code\":\"UNAUTHORIZED\",\"message\":\"Valid Bearer access token required\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"code\":\"FORBIDDEN\",\"message\":\"Missing required permission\"}"
                            );
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
