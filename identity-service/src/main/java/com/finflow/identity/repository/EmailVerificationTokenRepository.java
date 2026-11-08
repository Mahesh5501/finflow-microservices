package com.finflow.identity.repository;

import com.finflow.identity.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for one-time email-verification tokens.
 */
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    /**
     * Atomically consumes an email-verification token if it is still valid.
     * @return 1 if consumed successfully; otherwise 0.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update EmailVerificationToken t
               set t.used = true
             where t.tokenHash = :tokenHash
               and t.used = false
               and t.expiresAt > :now
            """)
    int consumeIfUsable(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from EmailVerificationToken t
             where t.expiresAt < :cutoff
                or t.used = true
            """)
    int deleteExpiredOrUsed(@Param("cutoff") Instant cutoff);
}
