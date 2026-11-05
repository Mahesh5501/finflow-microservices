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

    /** Every hour. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanup() {
        Instant now = Instant.now();
        int refresh = refreshTokenRepository.deleteExpiredOrRevoked(now);
        int reset = passwordResetTokenRepository.deleteExpiredOrUsed(now);
        int verify = emailVerificationTokenRepository.deleteExpiredOrUsed(now);
        int invite = orgInviteTokenRepository.deleteExpiredOrUsed(now);
        log.info("Token cleanup done refresh={} reset={} verify={} invite={}", refresh, reset, verify, invite);
    }
}
