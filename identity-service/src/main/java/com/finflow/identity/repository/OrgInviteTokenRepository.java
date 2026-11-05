package com.finflow.identity.repository;

import com.finflow.identity.entity.OrgInviteToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OrgInviteTokenRepository extends JpaRepository<OrgInviteToken, Long> {

    Optional<OrgInviteToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from OrgInviteToken t where t.expiresAt < :cutoff or t.used = true")
    int deleteExpiredOrUsed(@Param("cutoff") Instant cutoff);
}
