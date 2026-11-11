
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;

/**
 * JWT authentication filter.
 * - Runs once per request and checks whether the request contains a valid Bearer access token.
 * - If the token is valid, it creates an authenticated principal and stores it in Spring Security's SecurityContext.
 * - An explicit local-only switch supplies a configured test principal instead; no unsigned token claims are trusted and existing access checks still run.
 * - Constructor guards fail startup when that switch is used outside the local profile or without a loopback bind. Re-enable it to restore signature/expiry checks.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    // Immutable startup configuration; local identity never comes from a request.
    private final CommerceSecurityProperties securityProperties;

    /**
     * Provides RSA public keys from the identity-service JWKS endpoint.
     * @return the JwksKeyProvider.
     */
    private final JwksKeyProvider jwksKeyProvider;

    /**
     * Constructor injection: Spring provides the key provider.
     * @param jwksKeyProvider the JwksKeyProvider.
     * @param securityProperties typed switch and fixed local test identity
     * @param environment active profiles/bind address used to guard local bypass
     */
    public JwtAuthFilter(JwksKeyProvider jwksKeyProvider, CommerceSecurityProperties securityProperties, Environment environment) {
        this.jwksKeyProvider = jwksKeyProvider;
        this.securityProperties = securityProperties;
        if (!securityProperties.jwtValidationEnabled()) {
            // Fail startup rather than accidentally expose a token-free service. A profile alone is insufficient: the server must also bind loopback.
            boolean localOnly = environment.acceptsProfiles(Profiles.of("local")) && Arrays.stream(environment.getActiveProfiles()).allMatch("local"::equals);
            String address = environment.getProperty("server.address", "");
            if (!localOnly || !("127.0.0.1".equals(address) || "::1".equals(address))) {
                throw new IllegalStateException("JWT validation may be disabled only with the local profile alone and a loopback server.address");
            }
            log.warn("Local testing: JWT validation disabled; requests use configured test user {}", securityProperties.localUserId());
        }
    }

    /**
     * Executes the JWT authentication logic for the current request.
     * @param request the HttpServletRequest.
     * @param response the HttpServletResponse.
     * @param filterChain the FilterChain.
     * @throws ServletException if a servlet exception occurs.
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (!securityProperties.jwtValidationEnabled()) {
            // No token is decoded or trusted. The same server-configured principal is used for every local request, including one with a stale Bearer header.
            // Existing HTTP permissions and application owner/org checks still run; simply using permitAll would leave controllers with null users.
            AuthenticatedUser user = securityProperties.localUser();
            var authentication = new UsernamePasswordAuthenticationToken(user, null, AuthorityUtils.createAuthorityList(user.permissions().toArray(String[]::new)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
            return;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION); //Read the HTTP Authorization header. Expected format: Bearer <JWT>.

        /**
         * Only attempt JWT parsing when the header uses Bearer authentication.
         */
        if (header != null && header.startsWith("Bearer ")) {

            try {
                /**
                 *
                 */
                String raw = header.substring(7); //Remove "Bearer " and keep the actual JWT string.

                /**
                 * Build the JWT parser.
                 */
                Claims claims = Jwts.parser()
                         // JJWT calls this locator to find the verification key.
                        .keyLocator(new LocatorAdapter<Key>() {
                            @Override
                            protected Key locate(JwsHeader header) {
                                return jwksKeyProvider.getKey(header.getKeyId()); //Find the RSA public key matching the JWT's kid.
                            }
                        })
                        .build() // Build the parser.
                        .parseSignedClaims(raw) // Parse and verify the signed JWT. A successful call returns its claims.
                        .getPayload(); //Extract the JWT payload.

                Long userId = Long.valueOf(claims.getSubject()); // The JWT subject (sub) contains the user ID.
                String email = claims.get("email", String.class); // Extract the email claim.
                List<String> permissions = stringList(claims.get("permissions")); // Extract permission values from the JWT.
                List<Long> orgIds = longList(claims.get("orgIds")); // Extract organisation IDs from the JWT.

                var principal = new AuthenticatedUser(userId, email, orgIds, permissions); // Build the application-specific authenticated principal.

                /**
                 * Create a Spring Security authentication object.
                 */
                /**
                 * Principal: authenticated caller.
                 * Credentials: null because JWT authentication is already done.
                 * Authorities: permissions used by Spring Security.
                 */
                var auth = new UsernamePasswordAuthenticationToken(principal, null, AuthorityUtils.createAuthorityList(permissions.toArray(String[]::new)));
                SecurityContextHolder.getContext().setAuthentication(auth); // Store authentication for the current request. Controllers can now access it through Authentication.

            } catch (Exception ignored) {
                /**
                 * If token parsing, signature verification, key lookup, or claim conversion fails, remove any existing authentication.
                 * The request continues through the filter chain.
                 * Protected endpoints will subsequently be rejected by Spring Security because the caller is unauthenticated.
                 */
                SecurityContextHolder.clearContext();
            }
        }
        // Continue executing the remaining filters and eventually the controller, if security authorises the request.
        filterChain.doFilter(request, response);
    }

    /**
     * Converts a collection of permission claim values into strings.
     * @param raw the raw claim. If the claim is missing or is not a collection, returns an empty list.
     */
    private static List<String> stringList(Object raw) {

        if (raw instanceof Collection<?> c) {
            return c.stream()
                    .map(String::valueOf)
                    .toList();
        }

        return List.of();
    }

    // Converts a collection of organisation IDs into Long values. If the claim is missing or is not a collection, returns an empty list.
    private static List<Long> longList(Object raw) {

        if (raw instanceof Collection<?> c) {
            return c.stream()
                    .map(v -> Long.valueOf(String.valueOf(v)))
                    .toList();
        }

        return List.of();
    }
}
