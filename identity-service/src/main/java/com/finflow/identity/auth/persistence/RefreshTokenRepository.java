package com.finflow.identity.auth.persistence;

import com.finflow.identity.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Persistence operations for refresh-token sessions.
 *
 * Only SHA-256 token hashes are stored in the database.
 * Raw refresh tokens are returned to clients but never persisted.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Atomically consumes a refresh token.
     *
     * The conditional UPDATE is important:
     * - Only an unrevoked token can be consumed.
     * - The token must not have expired.
     * - Concurrent requests cannot both successfully consume the same token.
     *
     * Returns:
     * 1 = token successfully consumed.
     * 0 = token was already consumed, revoked, expired, or not found.
     *
     * The caller must issue a replacement refresh token only when exactly one row was updated.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RefreshToken t
               set t.revoked = true
             where t.tokenHash = :tokenHash
               and t.revoked = false
               and t.expiresAt > :now
            """)
    int consumeIfUsable(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    /**
     * Revokes every active refresh session belonging to a user.
     * Used after password changes and password resets.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RefreshToken t
               set t.revoked = true
             where t.userId = :userId
               and t.revoked = false
            """)
    int revokeAllForUser(@Param("userId") Long userId);

    /**
     * Removes expired or revoked refresh-token records.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from RefreshToken t
             where t.expiresAt < :cutoff
                or t.revoked = true
            """)
    int deleteExpiredOrRevoked(@Param("cutoff") Instant cutoff);
}
