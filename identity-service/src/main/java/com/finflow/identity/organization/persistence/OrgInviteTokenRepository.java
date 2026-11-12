package com.finflow.identity.organization.persistence;

import com.finflow.identity.organization.domain.OrgInviteToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for one-time organization invitation tokens.
 */
public interface OrgInviteTokenRepository extends JpaRepository<OrgInviteToken, Long> {

    Optional<OrgInviteToken> findByTokenHash(String tokenHash);

    /**
     * Atomically consumes an invitation if it is still valid and unused.
     * @return 1 if consumed successfully; otherwise 0.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update OrgInviteToken t
               set t.used = true
             where t.tokenHash = :tokenHash
               and t.used = false
               and t.expiresAt > :now
            """)
    int consumeIfUsable(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    /**
     * Invalidates all previous outstanding invitations for this organisation and email before a replacement invitation is issued.
     * @return number of invitations invalidated.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update OrgInviteToken t
               set t.used = true
             where t.orgId = :orgId
               and t.email = :email
               and t.used = false
            """)
    int invalidateOutstanding(@Param("orgId") Long orgId, @Param("email") String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from OrgInviteToken t
             where t.expiresAt < :cutoff
                or t.used = true
            """)
    int deleteExpiredOrUsed(@Param("cutoff") Instant cutoff);
}
