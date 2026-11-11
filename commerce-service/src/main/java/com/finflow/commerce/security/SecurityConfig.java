
package com.finflow.commerce.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
 * Defines Spring Security configuration for Commerce Service.
 *
 * Configures stateless JWT-based authentication and permission-based authorisation.
 * Normal startup uses verified JWTs. Explicit guarded local mode supplies a fixed principal through the same filter; permissions and ownership checks still run.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CommerceSecurityProperties.class)
public class SecurityConfig {

    /**
     * The security chain owns this filter's order and SecurityContext lifecycle.
     * Disable its separate servlet registration so the component does not also run outside the chain.
     * This matters when local authentication supplies the principal before the existing permission/ownership checks execute.
     */
    @Bean
    FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Custom filter that extracts and verifies JWT access tokens.
     * @return the JwtAuthFilter.
     */
    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Constructor injection of the JWT filter.
     * @param jwtAuthFilter the JwtAuthFilter.
     */
    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * Creates the security filter chain.
     *
     * @param http the HttpSecurity.
     * @return the SecurityFilterChain.
     * @throws Exception if an exception occurs.
     * This method defines which requests are public, which permissions are required, and how security failures should be returned to API clients.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable()) // This application uses stateless Bearer-token authentication rather than browser-managed session cookies.
                .cors(Customizer.withDefaults()) //Enable CORS using Spring's configured defaults. Actual allowed origins/methods depend on CORS configuration.
                .httpBasic(basic -> basic.disable()) //Disable HTTP Basic authentication.
                .formLogin(form -> form.disable()) //Disable form-based login.
                // Do not create or use an HTTP session for authentication. Each request must carry its own access token.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Define authorisation rules for incoming requests.
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/actuator/health", "/actuator/info").permitAll() //Health and application-info endpoints are public.
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAuthority("commerce:read") //GET API requests require read permission.
                        .requestMatchers(HttpMethod.POST, "/api/v1/**").hasAuthority("commerce:write") //POST API requests require write permission.
                        .requestMatchers(HttpMethod.PUT, "/api/v1/**").hasAuthority("commerce:write") //PUT API requests require write permission.
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/**").hasAuthority("commerce:write") //PATCH API requests require write permission.
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasAuthority("commerce:write") //DELETE API requests require write permission.
                        .anyRequest().authenticated() //Any request not matched above must still be authenticated.
                )
                // Configure responses for authentication and authorisation failures.
                .exceptionHandling(ex -> ex
                        // Authentication entry point: used when the caller is not authenticated.
                        .authenticationEntryPoint((request, response, authException) -> {
                                    response.setStatus(401);
                                    response.setContentType("application/json");
                                    response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Valid Bearer access token required\"}");
                                }
                        )
                        // Access denied handler: used when the caller is authenticated
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                                    response.setStatus(403);
                                    response.setContentType("application/json");
                                    response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Missing required permission\"}");
                                }
                        )
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // Insert the custom JWT filter before Spring's UsernamePasswordAuthenticationFilter.

        return http.build(); // Build and return the configured security filter chain.
    }
}
