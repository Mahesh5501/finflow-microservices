package com.finflow.identity.repository;

import com.finflow.identity.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * One-time password-reset tokens (hashed).
 * Its separate repository keeps reset-token queries distinct from login sessions.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Atomically consumes a password-reset token if it is still valid.
     * @return 1 if consumed successfully; otherwise 0.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PasswordResetToken t
               set t.used = true
             where t.tokenHash = :tokenHash
               and t.used = false
               and t.expiresAt > :now
            """)
    int consumeIfUsable(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from PasswordResetToken t
             where t.expiresAt < :cutoff
                or t.used = true
            """)
    int deleteExpiredOrUsed(@Param("cutoff") Instant cutoff);
}
