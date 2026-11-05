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
import com.finflow.identity.security.JwtService;
import com.finflow.identity.security.TokenCrypto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

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
            return new RegisterResponse(
                    toUserProfileResponse(user, List.of(), List.of()),
                    mailService.isEnabled() ? null : rawVerify
            );
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("EMAIL_ALREADY_EXISTS", "Email already registered");
        }
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        EmailVerificationToken token = emailVerificationTokenRepository
                .findByTokenHash(TokenCrypto.sha256(rawToken))
                .orElseThrow(() -> ApiException.unauthorized("INVALID_TOKEN", "Invalid or expired verification token"));
        if (!token.isUsable()) {
            throw ApiException.unauthorized("INVALID_TOKEN", "Invalid or expired verification token");
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_TOKEN", "Invalid or expired verification token"));
        user.markEmailVerified();
        token.markUsed();
        auditService.record(user.getId(), null, "EMAIL_VERIFIED", "USER", String.valueOf(user.getId()), user.getEmail());
    }

    @Transactional
    public DevTokenResponse resendVerification(String email) {
        String message = "If the email exists and is unverified, a verification link was sent";
        String normalized = normalizeEmail(email);
        return userRepository.findByEmail(normalized)
                .filter(User::isActive)
                .filter(u -> !u.isEmailVerified())
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
                .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password"));

        if (!user.isActive() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password");
        }
        if (!user.isEmailVerified()) {
            throw ApiException.unauthorized("EMAIL_NOT_VERIFIED", "Verify your email before login");
        }
        AuthenticationResponse tokens = issueTokens(user);
        auditService.record(user.getId(), null, "LOGIN_SUCCESS", "USER", String.valueOf(user.getId()), normalized);
        return tokens;
    }

    @Transactional
    public AuthenticationResponse refresh(String rawRefreshToken) {
        RefreshToken stored = requireUsableRefresh(rawRefreshToken);
        stored.revoke();
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_TOKEN", "Invalid refresh token"));
        if (!user.isActive() || !user.isEmailVerified()) {
            throw ApiException.unauthorized("INVALID_TOKEN", "Invalid refresh token");
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
                    String raw = TokenCrypto.newRawToken();
                    passwordResetTokenRepository.save(new PasswordResetToken(
                            user.getId(),
                            TokenCrypto.sha256(raw),
                            Instant.now().plusMillis(passwordResetExpirationMs)
                    ));
                    mailService.send(user.getEmail(), "FinFlow password reset", "Reset token: " + raw);
                    return new DevTokenResponse(message, mailService.isEnabled() ? null : raw);
                })
                .orElseGet(() -> new DevTokenResponse(message, null));
    }

    @Transactional
    public void resetPassword(String rawResetToken, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHash(TokenCrypto.sha256(rawResetToken))
                .orElseThrow(() -> ApiException.unauthorized("INVALID_TOKEN", "Invalid or expired reset token"));
        if (!token.isUsable()) {
            throw ApiException.unauthorized("INVALID_TOKEN", "Invalid or expired reset token");
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_TOKEN", "Invalid or expired reset token"));
        user.changePassword(passwordEncoder.encode(newPassword));
        token.markUsed();
        refreshTokenRepository.revokeAllForUser(user.getId());
        auditService.record(user.getId(), null, "PASSWORD_RESET", "USER", String.valueOf(user.getId()), null);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + userId));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Current password is incorrect");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        refreshTokenRepository.revokeAllForUser(userId);
        auditService.record(userId, null, "PASSWORD_CHANGED", "USER", String.valueOf(userId), null);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + userId));
        OrgAccess access = loadOrgAccess(userId);
        return toUserProfileResponse(user, access.orgIds(), access.permissions());
    }

    private String issueEmailVerification(User user) {
        String raw = TokenCrypto.newRawToken();
        emailVerificationTokenRepository.save(new EmailVerificationToken(
                user.getId(),
                TokenCrypto.sha256(raw),
                Instant.now().plusMillis(emailVerificationExpirationMs)
        ));
        mailService.send(user.getEmail(), "Verify your FinFlow email", "Verification token: " + raw);
        return raw;
    }

    private AuthenticationResponse issueTokens(User user) {
        OrgAccess access = loadOrgAccess(user.getId());
        String accessJwt = jwtService.createAccessToken(
                user.getId(), user.getEmail(), access.orgIds(), access.permissions()
        );
        String rawRefresh = TokenCrypto.newRawToken();
        refreshTokenRepository.save(new RefreshToken(
                user.getId(),
                TokenCrypto.sha256(rawRefresh),
                Instant.now().plusMillis(refreshExpirationMs)
        ));
        return new AuthenticationResponse(
                accessJwt,
                rawRefresh,
                "Bearer",
                jwtService.getAccessExpirationSeconds(),
                user.getId(),
                user.getEmail(),
                access.orgIds(),
                access.permissions()
        );
    }

    private OrgAccess loadOrgAccess(Long userId) {
        List<Membership> memberships = membershipRepository.findByUserIdAndStatus(
                userId, MembershipStatus.ACTIVE
        );
        List<Long> orgIds = memberships.stream().map(Membership::getOrgId).distinct().toList();
        LinkedHashSet<String> permissions = new LinkedHashSet<>();
        for (Membership m : memberships) {
            permissions.addAll(m.getRole().permissions());
        }
        return new OrgAccess(orgIds, List.copyOf(permissions));
    }

    private RefreshToken requireUsableRefresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(TokenCrypto.sha256(rawRefreshToken))
                .orElseThrow(() -> ApiException.unauthorized("INVALID_TOKEN", "Invalid refresh token"));
        if (!stored.isUsable()) {
            throw ApiException.unauthorized("INVALID_TOKEN", "Invalid refresh token");
        }
        return stored;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase();
    }

    private static UserProfileResponse toUserProfileResponse(
            User user,
            List<Long> orgIds,
            List<String> permissions
    ) {
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
