
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

    /**
     * Logger used to record JWKS refresh success and failure.
     * @return the logger.
     */
    private static final Logger log =
            LoggerFactory.getLogger(JwksKeyProvider.class);

    /**
     * HTTP client used to call identity-service's JWKS endpoint.
     * @return the rest client.
     */
    private final RestClient restClient;

    /**
     * Jackson mapper used to parse the JSON JWKS response.
     * @return the object mapper.
     */
    private final ObjectMapper objectMapper;

    /**
     * URL from which the identity service publishes public keys.
     * @return the JWKS URL.
     */
    private final String jwksUrl;

    /**
     * Number of seconds for which the cached keys remain valid.
     * @return the cache TTL in seconds.
     */
    private final long cacheTtlSeconds;

    /**
     * Stores public keys using JWT key ID (kid) as the map key.
     * ConcurrentHashMap supports concurrent reads and updates from multiple request threads.
     * @return the keys by kid.
     */
    private final Map<String, RSAPublicKey> keysByKid =
            new ConcurrentHashMap<>();

    /**
     * Time at which the current key cache expires.
     * volatile makes updates visible across threads.
     * @return the cache expires at.
     */
    private volatile Instant cacheExpiresAt = Instant.EPOCH;

    /**
     * Spring injects ObjectMapper and reads the JWKS settings from application configuration.
     * @param objectMapper the object mapper.
     * @param jwksUrl the JWKS URL.
     * @param cacheTtlSeconds the cache TTL in seconds.
     */
    public JwksKeyProvider(
            ObjectMapper objectMapper,
            @Value("${identity.jwks-url}") String jwksUrl,
            @Value("${identity.jwks-cache-seconds:300}") long cacheTtlSeconds
    ) {
        this.objectMapper = objectMapper;
        this.jwksUrl = jwksUrl;
        this.cacheTtlSeconds = cacheTtlSeconds;

        // Create the HTTP client used to retrieve JWKS.
        this.restClient = RestClient.create();
    }

    /**
     * Returns the RSA public key for the supplied JWT key ID.
     * @param kid the JWT key ID.
     * @return the RSA public key.
     * First checks the current cache.
     * If the key is missing, forces a JWKS refresh and checks again.
     * @throws IllegalStateException if the key is not found.
     */
    public RSAPublicKey getKey(String kid) {

        /**
         * Refresh only when the cache has expired or is empty.
         * @param force true if the cache should be refreshed regardless of its expiry.
         */
        refreshIfNeeded(false);

        /**
         * Try to find the requested key in the cache.
         * @return the RSA public key.
         */
        RSAPublicKey key = keysByKid.get(kid);

        /**
         * The JWT may have been signed using a newly rotated key.
         * Force a refresh once if the requested kid is not cached.
         */
        // Force a refresh once if the requested kid is not cached.
        if (key == null) {
            refreshIfNeeded(true);
            key = keysByKid.get(kid);
        }

        // If the key is still missing, verification cannot continue.
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
        /**
         * A refresh is also unnecessary if the cache is empty only when this condition is false, so an empty cache is loaded.
         * @return void.
         */
        if (!force
                && Instant.now().isBefore(cacheExpiresAt)
                && !keysByKid.isEmpty()) {
            return;
        }

        try {
            // Call identity-service's JWKS endpoint.
            String body = restClient.get()
                    .uri(jwksUrl)
                    .retrieve()
                    .body(String.class);

            // Parse the JSON response.
            JsonNode root = objectMapper.readTree(body);

            // Build the refreshed key map separately.
            /**
             * Only after parsing do we replace the current cache.
             * @return the refreshed key map.
             */
            Map<String, RSAPublicKey> next = new ConcurrentHashMap<>();

            /**
             * JWKS contains a "keys" array.
             * @param jwk the JSON node representing the JWK.
             */
            for (JsonNode jwk : root.path("keys")) {

                /**
                 * Key ID used by JWT headers to identify the signing key.
                 * @return the key ID.
                 */
                String kid = jwk.path("kid").asText(null);

                /**
                 * This implementation supports RSA public keys only.
                 * @return void.
                 */
                if (kid == null
                        || !"RSA".equals(jwk.path("kty").asText())) {
                    continue;
                }

                // Convert the RSA modulus and exponent into a Java key.
                next.put(
                        kid,
                        toRsaPublicKey(
                                jwk.path("n").asText(),
                                jwk.path("e").asText()
                        )
                );
            }

            // Replace the existing cached keys with the refreshed keys.
            keysByKid.clear();
            keysByKid.putAll(next);

            // Set the next cache expiry time.
            cacheExpiresAt =
                    Instant.now().plusSeconds(cacheTtlSeconds);

            log.info(
                    "JWKS refreshed from {} keys={}",
                    jwksUrl,
                    keysByKid.size()
            );

        } catch (Exception ex) {

            // Log refresh failure.
            log.warn(
                    "JWKS refresh failed from {}: {}",
                    jwksUrl,
                    ex.getMessage()
            );

            /**
             * If there are no previously cached keys, the application
             * cannot verify JWT signatures, so fail the lookup.
             */
            /**
             * If cached keys exist, retain them and continue using them.
             * @return void.
             */
            if (keysByKid.isEmpty()) {
                throw new IllegalStateException(
                        "Unable to load JWKS from " + jwksUrl,
                        ex
                );
            }
        }
    }

    /**
     * Converts RSA modulus (n) and exponent (e) from Base64URL
     * encoded JWKS values into a Java RSAPublicKey.
     */
    private static RSAPublicKey toRsaPublicKey(
            String n,
            String e
    ) throws Exception {

        /**
         * Decode the Base64URL-encoded RSA modulus.
         */
        byte[] modulus = Base64.getUrlDecoder().decode(n);

        /**
         * Decode the Base64URL-encoded RSA public exponent.
         */
        byte[] exponent = Base64.getUrlDecoder().decode(e);

        /**
         * RSA public keys are mathematically represented by:
         * modulus (n) and public exponent (e).
         * Signum 1 ensures BigInteger treats the byte array as positive.
         */
        RSAPublicKeySpec spec = new RSAPublicKeySpec(
                new BigInteger(1, modulus),
                new BigInteger(1, exponent)
        );

        /**
         * Ask Java's RSA KeyFactory to construct the public key.
         * @return the RSA public key.
         */
        return (RSAPublicKey) KeyFactory
                .getInstance("RSA")
                .generatePublic(spec);
    }
}
