package com.finflow.identity.service;

import com.finflow.identity.repository.EmailVerificationTokenRepository;
import com.finflow.identity.repository.OrgInviteTokenRepository;
import com.finflow.identity.repository.PasswordResetTokenRepository;
import com.finflow.identity.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Ops hygiene: purge expired / used tokens on a schedule. */
@Component
public class TokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupJob.class);

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
        log.info("Token cleanup done refresh={} reset={} verify={} invite={}",
                deletedRefreshTokens, deletedResetTokens, deletedVerificationTokens, deletedInviteTokens);
    }
}
