
package com.finflow.identity.service;

import com.finflow.identity.dto.response.AuthenticationResponse;
import com.finflow.identity.dto.response.DevTokenResponse;
import com.finflow.identity.dto.response.RegisterResponse;
import com.finflow.identity.dto.response.UserProfileResponse;
import com.finflow.identity.entity.EmailVerificationToken;
import com.finflow.identity.entity.Membership;
import com.finflow.identity.entity.MembershipStatus;
import com.finflow.identity.entity.PasswordResetToken;
import com.finflow.identity.entity.RefreshToken;
import com.finflow.identity.entity.User;
import com.finflow.identity.exception.ApiException;
import com.finflow.identity.mail.MailService;
import com.finflow.identity.repository.EmailVerificationTokenRepository;
import com.finflow.identity.repository.MembershipRepository;
import com.finflow.identity.repository.PasswordResetTokenRepository;
import com.finflow.identity.repository.RefreshTokenRepository;
import com.finflow.identity.repository.UserRepository;
import com.finflow.identity.security.jwt.JwtService;
import com.finflow.identity.security.token.TokenCrypto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Coordinates account and session workflows.
 *
 * Repositories handle persistence, JwtService handles JWT operations,
 * MailService handles email delivery, and AuditService records security events.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final AuditService auditService;
    private final long refreshExpirationMs;
    private final long passwordResetExpirationMs;
    private final long emailVerificationExpirationMs;

    public AuthService(
            UserRepository userRepository,
            MembershipRepository membershipRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            MailService mailService,
            AuditService auditService,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs,
            @Value("${password-reset-expiration-ms}") long passwordResetExpirationMs,
            @Value("${email-verification-expiration-ms}") long emailVerificationExpirationMs
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.auditService = auditService;
        this.refreshExpirationMs = refreshExpirationMs;
        this.passwordResetExpirationMs = passwordResetExpirationMs;
        this.emailVerificationExpirationMs = emailVerificationExpirationMs;
    }

    @Transactional
    public RegisterResponse register(String email, String password) {
        String normalized = normalizeEmail(email);
        if (userRepository.existsByEmail(normalized)) {
            throw ApiException.conflict("EMAIL_ALREADY_EXISTS", "Email already registered");
        }
        try {
            User user = userRepository.save(new User(normalized, passwordEncoder.encode(password)));
            String rawVerify = issueEmailVerification(user);
            auditService.record(user.getId(), null, "USER_REGISTERED", "USER", String.valueOf(user.getId()), normalized);
            return new RegisterResponse(toUserProfileResponse(user, List.of(), List.of()),mailService.isEnabled() ? null : rawVerify);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("EMAIL_ALREADY_EXISTS", "Email already registered");
        }
    }

    /**
     * Verifies an email using an atomically consumed one-time token.
     */
    @Transactional
    public void verifyEmail(String rawToken) {
        String tokenHash = TokenCrypto.sha256(rawToken);

        EmailVerificationToken token = emailVerificationTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> ApiException.unauthorized(
                        "INVALID_TOKEN",
                        "Invalid or expired verification token"
                ));

        // Capture the user ID before the bulk update clears the persistence context.
        Long userId = token.getUserId();

        // The database decides whether this token can still be consumed.
        int consumed = emailVerificationTokenRepository.consumeIfUsable(tokenHash, Instant.now());

        if (consumed != 1) {
            throw ApiException.unauthorized("INVALID_TOKEN", "Invalid or expired verification token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized(
                        "INVALID_TOKEN",
                        "Invalid or expired verification token"
                ));

        user.markEmailVerified();

        auditService.record(
                user.getId(),
                null,
                "EMAIL_VERIFIED",
                "USER",
                String.valueOf(user.getId()),
                user.getEmail()
        );
    }

    @Transactional
    public DevTokenResponse resendVerification(String email) {
        String message = "If the email exists and is unverified, a verification link was sent";
        String normalized = normalizeEmail(email);
        return userRepository.findByEmail(normalized)
                .filter(User::isActive)
                .filter(user -> !user.isEmailVerified())
                .map(user -> {
                    String raw = issueEmailVerification(user);
                    return new DevTokenResponse(message, mailService.isEnabled() ? null : raw);
                })
                .orElseGet(() -> new DevTokenResponse(message, null));
    }

    @Transactional
    public AuthenticationResponse login(String email, String password) {
        String normalized = normalizeEmail(email);
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> ApiException.unauthorized(
                        "INVALID_CREDENTIALS",
                        "Invalid email or password"
                ));

        if (!user.isActive() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.unauthorized(
                    "INVALID_CREDENTIALS",
                    "Invalid email or password"
            );
        }

        if (!user.isEmailVerified()) {
            throw ApiException.unauthorized(
                    "EMAIL_NOT_VERIFIED",
                    "Verify your email before login"
            );
        }

        AuthenticationResponse tokens = issueTokens(user);

        auditService.record(
                user.getId(),
                null,
                "LOGIN_SUCCESS",
                "USER",
                String.valueOf(user.getId()),
                normalized
        );

        return tokens;
    }

    /**
     * Rotates a refresh token exactly once.
     *
     * The conditional database UPDATE is the authoritative check.
     * A preceding SELECT only obtains the user ID.
     */
    @Transactional
    public AuthenticationResponse refresh(String rawRefreshToken) {
        String tokenHash = TokenCrypto.sha256(rawRefreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> ApiException.unauthorized(
                        "INVALID_TOKEN",
                        "Invalid refresh token"
                ));

        Long userId = stored.getUserId();

        int consumed = refreshTokenRepository.consumeIfUsable(tokenHash, Instant.now());

        if (consumed != 1) {
            throw ApiException.unauthorized(
                    "INVALID_TOKEN",
                    "Invalid refresh token"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized(
                        "INVALID_TOKEN",
                        "Invalid refresh token"
                ));

        if (!user.isActive() || !user.isEmailVerified()) {
            throw ApiException.unauthorized(
                    "INVALID_TOKEN",
                    "Invalid refresh token"
            );
        }
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(TokenCrypto.sha256(rawRefreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public DevTokenResponse forgotPassword(String email) {
        String message = "If the email exists, a reset link was sent";
        String normalized = normalizeEmail(email);
        return userRepository.findByEmail(normalized)
                .filter(User::isActive)
                .map(user -> {
                    String rawResetToken = TokenCrypto.newRawToken();

                    passwordResetTokenRepository.save(
                            new PasswordResetToken(
                                    user.getId(),
                                    TokenCrypto.sha256(rawResetToken),
                                    Instant.now().plusMillis(passwordResetExpirationMs)
                            )
                    );

                    mailService.send(
                            user.getEmail(),
                            "FinFlow password reset",
                            """
                            You requested a FinFlow password reset.

                            Paste this reset token into Postman (Auth → Reset Password)
                            or your client reset-password API:

                            %s

                            If you did not request this, ignore this message.
                            """.formatted(rawResetToken).stripIndent().trim()
                    );

                    return new DevTokenResponse(message,mailService.isEnabled() ? null : rawResetToken);
                })
                .orElseGet(() -> new DevTokenResponse(message, null));
    }

    /**
     * Resets a password using an atomically consumed one-time token.
     *
     * Token consumption, password update, refresh-token revocation and audit recording participate in the same transaction.
     */
    @Transactional
    public void resetPassword(String rawResetToken, String newPassword) {
        String tokenHash = TokenCrypto.sha256(rawResetToken);

        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> ApiException.unauthorized(
                        "INVALID_TOKEN",
                        "Invalid or expired reset token"
                ));

        // Capture the user ID before the bulk update clears the persistence context.
        Long userId = token.getUserId();

        int consumed = passwordResetTokenRepository.consumeIfUsable(tokenHash, Instant.now());

        if (consumed != 1) {
            throw ApiException.unauthorized(
                    "INVALID_TOKEN",
                    "Invalid or expired reset token"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized(
                        "INVALID_TOKEN",
                        "Invalid or expired reset token"
                ));

        user.changePassword(passwordEncoder.encode(newPassword));

        refreshTokenRepository.revokeAllForUser(user.getId());

        auditService.record(
                user.getId(),
                null,
                "PASSWORD_RESET",
                "USER",
                String.valueOf(user.getId()),
                null
        );
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + userId));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.unauthorized(
                    "INVALID_CREDENTIALS",
                    "Current password is incorrect"
            );
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        refreshTokenRepository.revokeAllForUser(userId);

        auditService.record(
                userId,
                null,
                "PASSWORD_CHANGED",
                "USER",
                String.valueOf(userId),
                null
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + userId));
        OrgAccess access = loadOrgAccess(userId);

        return toUserProfileResponse(
                user,
                access.orgIds(),
                access.permissions()
        );
    }

    /**
     * Creates and persists a one-time email verification token. Only its hash is stored in the database.
     */
    private String issueEmailVerification(User user) {
        String rawVerificationToken = TokenCrypto.newRawToken();

        emailVerificationTokenRepository.save(
                new EmailVerificationToken(
                        user.getId(),
                        TokenCrypto.sha256(rawVerificationToken),
                        Instant.now().plusMillis(emailVerificationExpirationMs)
                )
        );

        mailService.send(
                user.getEmail(),
                "Verify your FinFlow email",
                """
                Welcome to FinFlow.

                Confirm this email address by pasting the verification token into Postman
                (Identity → Auth → Verify Email), or your client verify-email API:

                %s

                If you did not register, ignore this message.
                """.formatted(rawVerificationToken).stripIndent().trim()
        );

        return rawVerificationToken;
    }

    /**
     * Creates a short-lived access JWT and a persisted opaque refresh token.
     * The database stores only the SHA-256 hash of the refresh token.
     */
    private AuthenticationResponse issueTokens(User user) {
        OrgAccess access = loadOrgAccess(user.getId());

        String accessJwt = jwtService.createAccessToken(
                user.getId(),
                user.getEmail(),
                access.orgIds(),
                access.permissions()
        );
        String rawRefreshToken = TokenCrypto.newRawToken();

        refreshTokenRepository.save(
                new RefreshToken(
                        user.getId(),
                        TokenCrypto.sha256(rawRefreshToken),
                        Instant.now().plusMillis(refreshExpirationMs)
                )
        );

        return new AuthenticationResponse(
                accessJwt,
                rawRefreshToken,
                "Bearer",
                jwtService.getAccessExpirationSeconds(),
                user.getId(),
                user.getEmail(),
                access.orgIds(),
                access.permissions()
        );
    }

    /**
     * Builds the user's current organisation access information.
     *
     * Permissions are currently unioned across active memberships.
     * They are not scoped to individual organisations in this JWT model.
     */
    private OrgAccess loadOrgAccess(Long userId) {
        List<Membership> memberships = membershipRepository.findByUserIdAndStatus(
                userId,
                MembershipStatus.ACTIVE
        );

        List<Long> orgIds = memberships.stream()
                .map(Membership::getOrgId)
                .distinct()
                .toList();

        LinkedHashSet<String> permissions = new LinkedHashSet<>();

        for (Membership membership : memberships) {
            permissions.addAll(membership.getRole().permissions());
        }

        return new OrgAccess(orgIds,List.copyOf(permissions)
        );
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static UserProfileResponse toUserProfileResponse(User user, List<Long> orgIds, List<String> permissions) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                orgIds,
                permissions
        );
    }

    private record OrgAccess(List<Long> orgIds, List<String> permissions) {
    }
}
