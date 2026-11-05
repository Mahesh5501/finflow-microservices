package com.finflow.identity.repository;

import com.finflow.identity.entity.Membership;
import com.finflow.identity.entity.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Membership queries for org RBAC.
 * Market pattern: one Spring Data repository per aggregate/table — do not merge with User/Org.
 */
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    /** Orgs the user belongs to (JWT orgIds / list mine). */
    List<Membership> findByUserIdAndStatus(Long userId, MembershipStatus status);

    /** Members of one org (list members / last-admin check). */
    List<Membership> findByOrgIdAndStatus(Long orgId, MembershipStatus status);

    /** Lookup existing User↔Org row (invite / accept / role change). */
    Optional<Membership> findByUserIdAndOrgId(Long userId, Long orgId);
}
