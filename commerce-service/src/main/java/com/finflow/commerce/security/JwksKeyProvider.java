
package com.finflow.commerce.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches JSON Web Key Set (JWKS) from identity-service.
 * It converts RSA public-key information into Java RSAPublicKey objects and caches them using the JWT key ID (kid).
 */
@Component
public class JwksKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(JwksKeyProvider.class); // Logger used to record JWKS refresh success and failure.

    private final RestClient restClient; // HTTP client used to call identity-service's JWKS endpoint.
    private final ObjectMapper objectMapper; // Jackson mapper used to parse the JSON JWKS response.
    private final String jwksUrl; // URL from which the identity service publishes public keys.
    private final long cacheTtlSeconds; // Number of seconds for which the cached keys remain valid.
    private final Map<String, RSAPublicKey> keysByKid = new ConcurrentHashMap<>(); // Stores public keys using JWT key ID (kid) as the map key. ConcurrentHashMap supports concurrent reads and updates from multiple request threads.
    private volatile Instant cacheExpiresAt = Instant.EPOCH; // Time at which the current key cache expires. volatile makes updates visible across threads.

    public JwksKeyProvider(ObjectMapper objectMapper, @Value("${identity.jwks-url}") String jwksUrl, @Value("${identity.jwks-cache-seconds:300}") long cacheTtlSeconds) {
        // Spring injects ObjectMapper and reads the JWKS settings from application configuration.
        this.objectMapper = objectMapper;
        this.jwksUrl = jwksUrl;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.restClient = RestClient.create();
    }

    // Returns the RSA public key for the supplied JWT key ID. First checks the current cache. If the key is missing, forces a JWKS refresh and checks again.
    public RSAPublicKey getKey(String kid) {
        refreshIfNeeded(false); // Refresh only when the cache has expired or is empty.
        RSAPublicKey key = keysByKid.get(kid); // Try to find the requested key in the cache.
        if (key == null) {
            refreshIfNeeded(true);
            key = keysByKid.get(kid); // Try to find the requested key in the cache again.
        }
        if (key == null) {
            throw new IllegalStateException("No JWKS key for kid=" + kid);
        }

        return key;
    }

    /**
     * Fetches and refreshes JWKS when necessary.
     * @param force true if the cache should be refreshed regardless of its expiry.
     * synchronized ensures that two threads do not refresh the same key cache at the same time.
     * force=true bypasses the normal cache-expiry check.
     */
    private synchronized void refreshIfNeeded(boolean force) {

        // Reuse cached keys when they are still valid.
        if (!force && Instant.now().isBefore(cacheExpiresAt) && !keysByKid.isEmpty()) {
            return;
        }

        try {
            // Call identity-service's JWKS endpoint.
            String body = restClient.get()
                    .uri(jwksUrl)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);

            // Build the refreshed key map separately.
            Map<String, RSAPublicKey> next = new ConcurrentHashMap<>();

            for (JsonNode jwk : root.path("keys")) {

                String kid = jwk.path("kid").asText(null); // Get the key ID from the JWK.

                // This implementation supports RSA public keys only.
                if (kid == null || !"RSA".equals(jwk.path("kty").asText())) {
                    continue;
                }

                // Convert the RSA modulus and exponent into a Java key.
                next.put(kid, toRsaPublicKey(jwk.path("n").asText(), jwk.path("e").asText()));
            }

            // Replace the existing cached keys with the refreshed keys.
            keysByKid.clear();
            keysByKid.putAll(next);
            cacheExpiresAt = Instant.now().plusSeconds(cacheTtlSeconds); // Set the next cache expiry time.

            log.info("JWKS refreshed from {} keys={}", jwksUrl, keysByKid.size()); // Log the refresh success.

        } catch (Exception ex) {

            // Log refresh failure.
            log.warn("JWKS refresh failed from {}: {}", jwksUrl, ex.getMessage()); // Log the refresh failure.

            // If there are no previously cached keys, the application cannot verify JWT signatures, so fail the lookup.
            if (keysByKid.isEmpty()) {
                throw new IllegalStateException("Unable to load JWKS from " + jwksUrl, ex); // Throw an exception if the JWKS cannot be loaded.
            }
        }
    }

    /**
     * Converts RSA modulus (n) and exponent (e) from Base64URL encoded JWKS values into a Java RSAPublicKey.
     * @param n the RSA modulus.
     * @param e the RSA exponent.
     * @return the RSA public key.
     * @throws Exception if the RSA public key cannot be created.
     */
    private static RSAPublicKey toRsaPublicKey(String n, String e) throws Exception {
        byte[] modulus = Base64.getUrlDecoder().decode(n); // Decode the Base64URL-encoded RSA modulus.
        byte[] exponent = Base64.getUrlDecoder().decode(e); // Decode the Base64URL-encoded RSA public exponent.
        RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1, exponent)); // Create the RSA public key specification.
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec); // Construct the RSA public key.
    }
}
