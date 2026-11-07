package com.finflow.identity.security.jwt;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;

/** Loads one durable private key; only local bootstrap may generate a missing key. */
final class SigningKeyStore {

    private SigningKeyStore() {
    }

    static KeyPair load(Path keyFile, boolean generateIfMissing) {
        Path absoluteFile = keyFile.toAbsolutePath().normalize();
        try {
            if (!Files.exists(absoluteFile)) {
                if (!generateIfMissing) {
                    throw new IOException("Required signing key file is missing");
                }
                createOnce(absoluteFile);
            }
            return read(absoluteFile);
        } catch (IOException | GeneralSecurityException | RuntimeException exception) {
            // Never recover from damaged/unreadable keys by rotating them implicitly.
            throw new IllegalStateException("Cannot load identity signing key; check jwt.signing-key-path and file permissions",
                    exception);
        }
    }

    private static void createOnce(Path keyFile) throws IOException, GeneralSecurityException {
        Path directory = keyFile.getParent();
        Files.createDirectories(directory);
        Path lockFile = keyFile.resolveSibling(keyFile.getFileName() + ".lock");
        // Coordinate initial creation across local JVMs; existing provisioned keys need no write access.
        try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = channel.tryLock()) {
            if (lock == null) {
                throw new IOException("Another process is initializing the signing key; retry startup");
            }
            if (Files.exists(keyFile)) {
                return;
            }
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            FileAttribute<?>[] permissions = Files.getFileStore(directory).supportsFileAttributeView("posix")
                    ? new FileAttribute<?>[]{PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))}
                    : new FileAttribute<?>[0];
            Path temporaryFile = Files.createTempFile(directory, ".signing-key-", ".tmp", permissions);
            try {
                Files.write(temporaryFile, pair.getPrivate().getEncoded());
                // Publish complete bytes, so a second reader never sees a half-written key.
                // Unsupported atomic moves fail startup instead of falling back to a partial write.
                Files.move(temporaryFile, keyFile, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temporaryFile);
            }
        }
    }

    private static KeyPair read(Path keyFile) throws IOException, GeneralSecurityException {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        var decoded = factory.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(keyFile)));
        if (!(decoded instanceof RSAPrivateCrtKey privateKey) || privateKey.getModulus().bitLength() < 2048) {
            throw new GeneralSecurityException("Signing key must be an RSA CRT private key of at least 2048 bits");
        }
        // Deriving the public key avoids separate files that could accidentally become mismatched.
        var publicKey = factory.generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
        return new KeyPair(publicKey, privateKey);
    }
}
