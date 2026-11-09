
package com.finflow.commerce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.Collection;
import java.util.List;

/**
 * JWT authentication filter.
 *
 * Runs once per request and checks whether the request contains
 * a valid Bearer access token.
 *
 * If the token is valid, it creates an authenticated principal
 * and stores it in Spring Security's SecurityContext.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    // Provides RSA public keys from the identity-service JWKS endpoint.
    private final JwksKeyProvider jwksKeyProvider;

    // Constructor injection: Spring provides the key provider.
    public JwtAuthFilter(JwksKeyProvider jwksKeyProvider) {
        this.jwksKeyProvider = jwksKeyProvider;
    }

    /**
     * Executes the JWT authentication logic for the current request.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Read the HTTP Authorization header.
        // Expected format: Bearer <JWT>.
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Only attempt JWT parsing when the header uses Bearer authentication.
        if (header != null && header.startsWith("Bearer ")) {

            try {
                // Remove "Bearer " and keep the actual JWT string.
                String raw = header.substring(7);

                // Build the JWT parser.
                Claims claims = Jwts.parser()

                        // JJWT calls this locator to find the verification key.
                        // The JWT header contains the key ID (kid).
                        .keyLocator(new LocatorAdapter<Key>() {

                            @Override
                            protected Key locate(JwsHeader header) {

                                // Find the RSA public key matching the JWT's kid.
                                return jwksKeyProvider.getKey(
                                        header.getKeyId()
                                );
                            }
                        })

                        // Build the parser.
                        .build()

                        // Parse and verify the signed JWT.
                        // A successful call returns its claims.
                        .parseSignedClaims(raw)

                        // Extract the JWT payload.
                        .getPayload();

                // The JWT subject (sub) contains the user ID.
                Long userId = Long.valueOf(claims.getSubject());

                // Extract the email claim.
                String email = claims.get("email", String.class);

                // Extract permission values from the JWT.
                List<String> permissions =
                        stringList(claims.get("permissions"));

                // Extract organisation IDs from the JWT.
                List<Long> orgIds =
                        longList(claims.get("orgIds"));

                // Build the application-specific authenticated principal.
                var principal = new AuthenticatedUser(
                        userId,
                        email,
                        orgIds,
                        permissions
                );

                // Create a Spring Security authentication object.
                //
                // Principal: authenticated caller.
                // Credentials: null because JWT authentication is already done.
                // Authorities: permissions used by Spring Security.
                var auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        AuthorityUtils.createAuthorityList(
                                permissions.toArray(String[]::new)
                        )
                );

                // Store authentication for the current request.
                // Controllers can now access it through Authentication.
                SecurityContextHolder.getContext()
                        .setAuthentication(auth);

            } catch (Exception ignored) {

                // If token parsing, signature verification, key lookup,
                // or claim conversion fails, remove any existing authentication.
                //
                // The request continues through the filter chain.
                // Protected endpoints will subsequently be rejected
                // by Spring Security because the caller is unauthenticated.
                SecurityContextHolder.clearContext();
            }
        }

        // Continue executing the remaining filters and eventually
        // the controller, if security authorises the request.
        filterChain.doFilter(request, response);
    }

    /**
     * Converts a collection of permission claim values into strings.
     *
     * If the claim is missing or is not a collection,
     * returns an empty list.
     */
    private static List<String> stringList(Object raw) {

        if (raw instanceof Collection<?> c) {
            return c.stream()
                    .map(String::valueOf)
                    .toList();
        }

        return List.of();
    }

    /**
     * Converts a collection of organisation IDs into Long values.
     *
     * If the claim is missing or is not a collection,
     * returns an empty list.
     */
    private static List<Long> longList(Object raw) {

        if (raw instanceof Collection<?> c) {
            return c.stream()
                    .map(v -> Long.valueOf(String.valueOf(v)))
                    .toList();
        }

        return List.of();
    }
}
