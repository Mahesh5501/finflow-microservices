package com.finflow.commerce.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Uses real RSA signatures and an in-memory key provider; no live Identity service. */
class JwtAuthFilterTest {

    private static KeyPair signingKey;

    @BeforeAll
    static void createKey() {
        signingKey = Jwts.SIG.RS256.keyPair().build();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void omittedToggleDefaultsToValidationEnabled() {
        new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class).run(context -> {
            var properties = context.getBean(CommerceSecurityProperties.class);
            assertTrue(properties.jwtValidationEnabled());
            assertEquals(1L, properties.localUserId());
            assertEquals(List.of(10L), properties.localOrganisationIds());
        });
    }

    @Test
    void disablingValidationOutsideLocalOrOnPublicAddressFailsStartup() {
        var properties = new CommerceSecurityProperties(false, 1L, List.of(10L));
        assertThrows(IllegalStateException.class,
                () -> new JwtAuthFilter(provider(), properties, new MockEnvironment()));
        MockEnvironment publicLocal = new MockEnvironment().withProperty("server.address", "0.0.0.0");
        publicLocal.setActiveProfiles("local");
        assertThrows(IllegalStateException.class,
                () -> new JwtAuthFilter(provider(), properties, publicLocal));
        MockEnvironment mixed = localEnvironment();
        mixed.setActiveProfiles("local", "prod");
        assertThrows(IllegalStateException.class,
                () -> new JwtAuthFilter(provider(), properties, mixed));
    }

    @Test
    void localIdentityComesFromConfigurationNotUnverifiedTokenOrHeaders() throws Exception {
        var filter = new JwtAuthFilter(provider(),
                new CommerceSecurityProperties(false, 123L, List.of(456L)), localEnvironment());
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer unverified-token");
        request.addHeader("X-User-Id", "999");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        var user = (AuthenticatedUser) authentication.getPrincipal();
        assertEquals(123L, user.userId());
        assertEquals(List.of(456L), user.orgIds());
        assertEquals(List.of("commerce:read", "commerce:write"), user.permissions());
    }

    @Test
    void enabledValidationAuthenticatesRealSignedClaims() throws Exception {
        authenticate(token(signingKey, Instant.now().plusSeconds(60)));
        var user = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals(7L, user.userId());
        assertEquals(List.of(10L), user.orgIds());
        assertEquals(List.of("commerce:read"), user.permissions());
        assertFalse(user.permissions().contains("commerce:write"));
    }

    @Test
    void enabledValidationRejectsForgedSignatureAndExpiredToken() throws Exception {
        KeyPair otherKey = Jwts.SIG.RS256.keyPair().build();
        authenticate(token(otherKey, Instant.now().plusSeconds(60)));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        authenticate(token(signingKey, Instant.now().minusSeconds(60)));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void enabledValidationDoesNotSupplyLocalIdentityWhenTokenIsAbsent() throws Exception {
        var filter = new JwtAuthFilter(provider(),
                new CommerceSecurityProperties(true, 123L, List.of(456L)), localEnvironment());
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private static void authenticate(String token) throws Exception {
        var filter = new JwtAuthFilter(provider(),
                new CommerceSecurityProperties(true, 1L, List.of(10L)), new MockEnvironment());
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    private static String token(KeyPair key, Instant expires) {
        return Jwts.builder().header().keyId("test-key").and().subject("7")
                .claim("email", "test@example.com").claim("orgIds", List.of(10L))
                .claim("permissions", List.of("commerce:read"))
                .expiration(Date.from(expires)).signWith(key.getPrivate(), Jwts.SIG.RS256).compact();
    }

    private static JwksKeyProvider provider() {
        return new JwksKeyProvider(new ObjectMapper(), "http://unused.invalid/jwks", 300L) {
            @Override
            public RSAPublicKey getKey(String kid) {
                return (RSAPublicKey) signingKey.getPublic();
            }
        };
    }

    private static MockEnvironment localEnvironment() {
        MockEnvironment environment = new MockEnvironment().withProperty("server.address", "127.0.0.1");
        environment.setActiveProfiles("local");
        return environment;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CommerceSecurityProperties.class)
    static class PropertiesConfig {
    }
}
