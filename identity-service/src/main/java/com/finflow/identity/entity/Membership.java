package com.finflow.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * User ↔ Org link with one role.
 *
 * <pre>
 * create org     → ACTIVE + ORG_ADMIN
 * invite member  → INVITED (pending)
 * accept invite  → ACTIVE
 * remove member  → REMOVED
 * </pre>
 */
@Entity
@Table(
        name = "membership",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_membership_user_org",
                columnNames = {"user_id", "org_id"}
        )
)
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 40)
    private OrgRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MembershipStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Membership() {
    }

    /** Active membership (org creator / after invite accepted). */
    public Membership(Long userId, Long orgId, OrgRole role) {
        this.userId = userId;
        this.orgId = orgId;
        this.role = role;
        this.status = MembershipStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    /** New invite row (status = INVITED). */
    public static Membership invited(Long userId, Long orgId, OrgRole role) {
        Membership m = new Membership(userId, orgId, role);
        m.status = MembershipStatus.INVITED;
        return m;
    }

    public void changeRole(OrgRole role) {
        this.role = role;
    }

    /** Soft-delete — keeps history, blocks access. */
    public void remove() {
        this.status = MembershipStatus.REMOVED;
    }

    /** Re-invite a removed (or already invited) user. */
    public void markInvited(OrgRole role) {
        this.role = role;
        this.status = MembershipStatus.INVITED;
    }

    /** Invite accepted → full member. */
    public void acceptInvite() {
        this.status = MembershipStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE;
    }

    public boolean isInvited() {
        return status == MembershipStatus.INVITED;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public OrgRole getRole() {
        return role;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
