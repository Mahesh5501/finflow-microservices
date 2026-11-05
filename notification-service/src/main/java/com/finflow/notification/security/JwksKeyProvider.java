package com.finflow.notification.security;

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

@Component
public class JwksKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(JwksKeyProvider.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String jwksUrl;
    private final long cacheTtlSeconds;
    private final Map<String, RSAPublicKey> keysByKid = new ConcurrentHashMap<>();
    private volatile Instant cacheExpiresAt = Instant.EPOCH;

    public JwksKeyProvider(
            ObjectMapper objectMapper,
            @Value("${identity.jwks-url}") String jwksUrl,
            @Value("${identity.jwks-cache-seconds:300}") long cacheTtlSeconds
    ) {
        this.objectMapper = objectMapper;
        this.jwksUrl = jwksUrl;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.restClient = RestClient.create();
    }

    public RSAPublicKey getKey(String kid) {
        refreshIfNeeded(false);
        RSAPublicKey key = keysByKid.get(kid);
        if (key == null) {
            refreshIfNeeded(true);
            key = keysByKid.get(kid);
        }
        if (key == null) {
            throw new IllegalStateException("No JWKS key for kid=" + kid);
        }
        return key;
    }

    private synchronized void refreshIfNeeded(boolean force) {
        if (!force && Instant.now().isBefore(cacheExpiresAt) && !keysByKid.isEmpty()) {
            return;
        }
        try {
            String body = restClient.get().uri(jwksUrl).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(body);
            Map<String, RSAPublicKey> next = new ConcurrentHashMap<>();
            for (JsonNode jwk : root.path("keys")) {
                String kid = jwk.path("kid").asText(null);
                if (kid == null || !"RSA".equals(jwk.path("kty").asText())) {
                    continue;
                }
                next.put(kid, toRsaPublicKey(jwk.path("n").asText(), jwk.path("e").asText()));
            }
            keysByKid.clear();
            keysByKid.putAll(next);
            cacheExpiresAt = Instant.now().plusSeconds(cacheTtlSeconds);
            log.info("JWKS refreshed from {} keys={}", jwksUrl, keysByKid.size());
        } catch (Exception ex) {
            log.warn("JWKS refresh failed from {}: {}", jwksUrl, ex.getMessage());
            if (keysByKid.isEmpty()) {
                throw new IllegalStateException("Unable to load JWKS from " + jwksUrl, ex);
            }
        }
    }

    private static RSAPublicKey toRsaPublicKey(String n, String e) throws Exception {
        byte[] modulus = Base64.getUrlDecoder().decode(n);
        byte[] exponent = Base64.getUrlDecoder().decode(e);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1, exponent));
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
