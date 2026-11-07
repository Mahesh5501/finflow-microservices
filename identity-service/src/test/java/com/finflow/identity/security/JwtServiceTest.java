package com.finflow.identity.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    @TempDir
    Path directory;

    @Test
    void restartKeepsKeyIdPublicKeyAndExistingTokenValidity() throws Exception {
        Path file = directory.resolve("keys/signing.pk8");
        JwtService first = service(file, true);
        String token = token(first);
        byte[] original = Files.readAllBytes(file);
        JwtService restarted = service(file, false);
        assertEquals(first.jwks(), restarted.jwks());
        assertEquals("42", restarted.parse(token).getSubject());
        assertEquals("42", first.parse(token(restarted)).getSubject());
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    void independentJvmCanVerifyTokenWrittenByPreviousJvm() throws Exception {
        Path file = directory.resolve("process.pk8");
        Path tokenFile = directory.resolve("access-token.txt");
        runProcess("issue", file, tokenFile);
        runProcess("verify", file, tokenFile);
    }

    @Test
    void jwksCanVerifyRs256TokenWithoutPrivateMaterial() throws Exception {
        JwtService service = service(directory.resolve("key.pk8"), true);
        Map<?, ?> key = (Map<?, ?>) ((List<?>) service.jwks().get("keys")).getFirst();
        assertEquals("RSA", key.get("kty"));
        assertEquals("RS256", key.get("alg"));
        assertEquals("sig", key.get("use"));
        assertEquals(6, key.size());
        assertFalse(key.containsKey("d"));
        Base64.Decoder decoder = Base64.getUrlDecoder();
        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                new BigInteger(1, decoder.decode((String) key.get("n"))),
                new BigInteger(1, decoder.decode((String) key.get("e")))));
        var jwt = Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token(service));
        assertEquals(key.get("kid"), jwt.getHeader().getKeyId());
        assertEquals("RS256", jwt.getHeader().getAlgorithm());
        assertEquals(List.of("wallets:read"), jwt.getPayload().get("permissions"));
    }

    @Test
    void missingRequiredKeyFailsWithoutCreatingAnything() {
        Path file = directory.resolve("missing/key.pk8");
        assertThrows(IllegalStateException.class, () -> service(file, false));
        assertFalse(Files.exists(file.getParent()));
    }

    @Test
    void corruptedExistingKeyIsNotReplacedEvenWhenGenerationIsEnabled() throws Exception {
        Path file = directory.resolve("broken.pk8");
        byte[] broken = "not a private key".getBytes(StandardCharsets.UTF_8);
        Files.write(file, broken);
        assertThrows(IllegalStateException.class, () -> service(file, true));
        assertArrayEquals(broken, Files.readAllBytes(file));
    }

    @Test
    void nonRsaPrivateKeyIsRejected() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        Path file = directory.resolve("ec.pk8");
        Files.write(file, generator.generateKeyPair().getPrivate().getEncoded());
        assertThrows(IllegalStateException.class, () -> service(file, true));
    }

    @Test
    void undersizedRsaKeyIsRejected() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        Path file = directory.resolve("weak.pk8");
        Files.write(file, generator.generateKeyPair().getPrivate().getEncoded());
        assertThrows(IllegalStateException.class, () -> service(file, true));
    }

    @Test
    void provisionedKeyNeedsNoLockFileOrWriteOnLoad() throws Exception {
        Path original = directory.resolve("original.pk8");
        JwtService first = service(original, true);
        Path provisioned = directory.resolve("mounted.pk8");
        Files.copy(original, provisioned);
        assertEquals(first.jwks(), service(provisioned, false).jwks());
        assertFalse(Files.exists(directory.resolve("mounted.pk8.lock")));
    }

    @Test
    void differentKeyRejectsOldTokensAndHasDifferentKid() {
        JwtService first = service(directory.resolve("one.pk8"), true);
        JwtService other = service(directory.resolve("two.pk8"), true);
        assertNotEquals(first.jwks(), other.jwks());
        assertThrows(SignatureException.class, () -> other.parse(token(first)));
    }

    @Test
    void persistenceDoesNotDisableTokenExpiry() {
        Path file = directory.resolve("expired.pk8");
        JwtService expired = new JwtService(-1000, file.toString(), true);
        JwtService restarted = service(file, false);
        assertThrows(ExpiredJwtException.class, () -> restarted.parse(token(expired)));
    }

    @Test
    void directoryCannotBeUsedAsKeyFile() {
        assertThrows(IllegalStateException.class, () -> service(directory, true));
    }

    @Test
    void overlappingBootstrapLockFailsWithoutPublishingAnotherKey() throws Exception {
        Path file = directory.resolve("locked.pk8");
        try (FileChannel channel = FileChannel.open(directory.resolve("locked.pk8.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            assertTrue(lock.isValid());
            assertThrows(IllegalStateException.class, () -> service(file, true));
            assertFalse(Files.exists(file));
        }
    }

    @Test
    void prodProfileRefusesToGenerateAMissingKey() {
        Path file = directory.resolve("prod-missing.pk8");
        prodContext(file).run(context -> {
            assertTrue(context.getStartupFailure() != null);
            assertFalse(Files.exists(file));
        });
    }

    @Test
    void prodProfileLoadsProvisionedKey() {
        Path file = directory.resolve("prod-mounted.pk8");
        JwtService provisioned = service(file, true);
        prodContext(file).run(context -> {
            assertEquals(null, context.getStartupFailure());
            JwtService loaded = context.getBean(JwtService.class);
            assertEquals(provisioned.jwks(), loaded.jwks());
            assertEquals("42", loaded.parse(token(provisioned)).getSubject());
        });
    }

    private ApplicationContextRunner prodContext(Path file) {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=prod", "IDENTITY_SIGNING_KEY_PATH=" + file)
                .withUserConfiguration(JwtService.class);
    }

    private JwtService service(Path file, boolean generate) {
        return new JwtService(900000, file.toString(), generate);
    }

    private static String token(JwtService service) {
        return service.createAccessToken(42L, "restart@finflow.test", List.of(7L), List.of("wallets:read"));
    }

    private void runProcess(String mode, Path keyFile, Path tokenFile) throws Exception {
        Path output = directory.resolve(mode + ".log");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"), RestartProcess.class.getName(),
                mode, keyFile.toString(), tokenFile.toString())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Independent JVM timed out");
            assertEquals(0, process.exitValue(), () -> {
                try {
                    return Files.readString(output);
                } catch (Exception exception) {
                    return "Cannot read child JVM result";
                }
            });
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly().waitFor();
            }
        }
    }

    /** Two independent JVMs share only the temporary key/token files, not application memory. */
    public static class RestartProcess {
        public static void main(String[] args) throws Exception {
            JwtService service = new JwtService(900000, args[1], "issue".equals(args[0]));
            if ("issue".equals(args[0])) {
                Files.writeString(Path.of(args[2]), token(service));
            } else if (!"42".equals(service.parse(Files.readString(Path.of(args[2]))).getSubject())) {
                throw new IllegalStateException("Token subject did not survive restart");
            }
        }
    }
}
