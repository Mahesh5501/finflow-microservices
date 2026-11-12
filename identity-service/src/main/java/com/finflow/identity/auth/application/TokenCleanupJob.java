package com.finflow.identity.auth.application;

import com.finflow.identity.auth.persistence.EmailVerificationTokenRepository;
import com.finflow.identity.organization.persistence.OrgInviteTokenRepository;
import com.finflow.identity.auth.persistence.PasswordResetTokenRepository;
import com.finflow.identity.auth.persistence.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Ops hygiene: purge expired / used tokens on a schedule. */
@Slf4j
@Component
public class TokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final OrgInviteTokenRepository orgInviteTokenRepository;

    public TokenCleanupJob(
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            OrgInviteTokenRepository orgInviteTokenRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.orgInviteTokenRepository = orgInviteTokenRepository;
    }

    /** Bulk deletes run hourly in a transaction, avoiding one delete call per expired token. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanup() {
        Instant now = Instant.now();
        int deletedRefreshTokens = refreshTokenRepository.deleteExpiredOrRevoked(now);
        int deletedResetTokens = passwordResetTokenRepository.deleteExpiredOrUsed(now);
        int deletedVerificationTokens = emailVerificationTokenRepository.deleteExpiredOrUsed(now);
        int deletedInviteTokens = orgInviteTokenRepository.deleteExpiredOrUsed(now);
        log.info("Token cleanup done refresh={} reset={} verify={} invite={}", deletedRefreshTokens, deletedResetTokens, deletedVerificationTokens, deletedInviteTokens);
    }
}
