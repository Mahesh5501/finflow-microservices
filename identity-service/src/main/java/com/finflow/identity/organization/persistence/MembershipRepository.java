package com.finflow.identity.organization.persistence;

import com.finflow.identity.organization.domain.Membership;
import com.finflow.identity.organization.domain.MembershipStatus;
import com.finflow.identity.organization.domain.OrgRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Membership queries for org RBAC.
 * Spring Data owns query implementation; permission and workflow rules stay in the service.
 */
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    /** Orgs the user belongs to (JWT orgIds / list mine). */
    List<Membership> findByUserIdAndStatus(Long userId, MembershipStatus status);

    /** Members of one org for the member-list response. */
    List<Membership> findByOrgIdAndStatus(Long orgId, MembershipStatus status);

    /** Database-side count avoids loading entities just to protect the last active admin. */
    long countByOrgIdAndStatusAndRole(Long orgId, MembershipStatus status, OrgRole role);

    /** Lookup the membership for a user and an organisation. */
    Optional<Membership> findByUserIdAndOrgId(Long userId, Long orgId);
}
