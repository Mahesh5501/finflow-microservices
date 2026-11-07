package com.finflow.identity.security;

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

/**
 * RS256 access JWTs + JWKS for other FinFlow services (wallet validates via public key).
 * Interview: asymmetric keys — private key stays in identity; public key is published.
 */
@Service
public class JwtService {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String keyId;
    private final long accessExpirationMs;

    public JwtService(
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${jwt.signing-key-path:.local/identity/signing-key.pk8}") String signingKeyPath,
            @Value("${jwt.generate-key-if-missing:true}") boolean generateKeyIfMissing
    ) {
        try {
            KeyPair pair = SigningKeyStore.load(Path.of(signingKeyPath), generateKeyIfMissing);
            this.privateKey = (RSAPrivateKey) pair.getPrivate();
            this.publicKey = (RSAPublicKey) pair.getPublic();
            // Same persisted key => same kid across restarts/replicas, without a second metadata file.
            this.keyId = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded()));
            this.accessExpirationMs = accessExpirationMs;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize persistent JWT signing key", e);
        }
    }

    public String createAccessToken(Long userId, String email, List<Long> orgIds, List<String> permissions) {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(keyId).and()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("orgIds", orgIds)
                .claim("permissions", permissions)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpirationMs)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public List<String> permissions(Claims claims) {
        Object raw = claims.get("permissions");
        if (raw instanceof Collection<?> c) {
            return c.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }

    /** RFC 7517 JWKS — wallet/other services fetch this to verify access tokens. */
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
        // Strip leading zero sign byte if present.
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
