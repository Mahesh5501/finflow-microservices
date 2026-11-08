package com.finflow.identity.security.jwt;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.LinkOption;
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

final class SigningKeyStore {

    private static final int MAX_KEY_FILE_SIZE = 16 * 1024;

    private SigningKeyStore() {
    }

    static KeyPair load(Path keyFile, boolean generateIfMissing) {
        Path absoluteFile = keyFile.toAbsolutePath().normalize();
        try {
            if (!Files.exists(absoluteFile, LinkOption.NOFOLLOW_LINKS)) {
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
        if (directory == null) {
            throw new IOException("Signing key directory is invalid");
        }

        Files.createDirectories(directory);
        Path lockFile = keyFile.resolveSibling(keyFile.getFileName() + ".lock");
        // Coordinate initial creation across local JVMs; existing provisioned keys need no write access.
        try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = channel.tryLock()) {
            if (lock == null) {
                throw new IOException("Another process is initializing the signing key; retry startup");
            }

            if (Files.exists(keyFile, LinkOption.NOFOLLOW_LINKS)) {
                return;
            }
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();

            FileAttribute<?>[] permissions =
                    Files.getFileStore(directory).supportsFileAttributeView("posix")
                            ? new FileAttribute<?>[]{
                            PosixFilePermissions.asFileAttribute(
                                    PosixFilePermissions.fromString("rw-------")
                            )
                    }
                            : new FileAttribute<?>[0];

            Path temporaryFile = Files.createTempFile(
                    directory,
                    ".signing-key-",
                    ".tmp",
                    permissions
            );

            try {
                Files.write(temporaryFile, pair.getPrivate().getEncoded());

                // Publish the complete key atomically.
                Files.move(
                        temporaryFile,
                        keyFile,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } finally {
                Files.deleteIfExists(temporaryFile);
            }
        }
    }

    private static KeyPair read(Path keyFile) throws IOException, GeneralSecurityException {

        if (Files.isSymbolicLink(keyFile)) {
            throw new IOException("Signing key must not be a symbolic link");
        }

        if (!Files.isRegularFile(keyFile, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Signing key path must be a regular file");
        }

        long fileSize = Files.size(keyFile);
        if (fileSize <= 0 || fileSize > MAX_KEY_FILE_SIZE) {
            throw new IOException("Signing key file has an invalid size");
        }

        byte[] encodedKey = Files.readAllBytes(keyFile);

        KeyFactory factory = KeyFactory.getInstance("RSA");

        var decoded = factory.generatePrivate( new PKCS8EncodedKeySpec(encodedKey));

        if (!(decoded instanceof RSAPrivateCrtKey privateKey) || privateKey.getModulus().bitLength() < 2048) {
            throw new GeneralSecurityException("Signing key must be an RSA CRT private key of at least 2048 bits");
        }

        // Derive the public key to prevent private/public key mismatches.
        var publicKey = factory.generatePublic(
                new RSAPublicKeySpec(
                        privateKey.getModulus(),
                        privateKey.getPublicExponent()
                )
        );

        return new KeyPair(publicKey, privateKey);
    }
}
