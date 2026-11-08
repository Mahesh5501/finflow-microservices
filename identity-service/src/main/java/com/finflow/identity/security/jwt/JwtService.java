package com.finflow.identity.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String keyId;
    private final long accessExpirationMs;

    public JwtService(
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${jwt.signing-key-path:.local/identity/signing-key.pk8}") String signingKeyPath,
            @Value("${jwt.generate-key-if-missing:true}") boolean generateKeyIfMissing) {
        if (accessExpirationMs <= 0) {
            throw new IllegalArgumentException("jwt.access-expiration-ms must be greater than zero");
        }

        try {
            KeyPair pair = SigningKeyStore.load(Path.of(signingKeyPath), generateKeyIfMissing);
            this.privateKey = (RSAPrivateKey) pair.getPrivate();
            this.publicKey = (RSAPublicKey) pair.getPublic();

            // Derive kid from the persisted public key so it remains stable across restarts.
            this.keyId = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded()));
            this.accessExpirationMs = accessExpirationMs;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize persistent JWT signing key", e);
        }
    }

    public String createAccessToken(Long userId, String email, List<Long> orgIds, List<String> permissions) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("JWT userId must be a positive value");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("JWT email must not be blank");
        }

        if (orgIds == null || permissions == null) {
            throw new IllegalArgumentException("JWT orgIds and permissions must not be null");
        }

        Instant now = Instant.now();
        return Jwts.builder()
                .header()
                .keyId(keyId)
                .and()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("orgIds", List.copyOf(orgIds))
                .claim("permissions", List.copyOf(permissions))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpirationMs)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims parse(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT must not be blank");
        }

        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public List<String> permissions(Claims claims) {
        Object raw = claims.get("permissions");

        if (!(raw instanceof Collection<?> collection)) {
            throw new IllegalArgumentException("JWT permissions claim is missing or invalid");
        }

        return collection.stream()
                .map(value -> {
                    if (!(value instanceof String permission) || permission.isBlank()) {
                        throw new IllegalArgumentException("JWT contains an invalid permission");
                    }
                    return permission;
                })
                .toList();
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }

    /**
     * RFC 7517 JWKS — other FinFlow services use this public key to verify access tokens.
     */
    public Map<String, Object> jwks() {
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("kid", keyId);
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("n", base64Url(publicKey.getModulus()));
        jwk.put("e", base64Url(publicKey.getPublicExponent()));
        return Map.of("keys", List.of(jwk));
    }

    private static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();

        // BigInteger may add a leading zero sign byte.
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
