package com.finflow.identity.organization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/** Invite-by-link token for org onboarding (hashed). */
@Getter
@Entity
@Table(name = "org_invite_token")
public class OrgInviteToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invite_id")
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 40)
    private OrgRole role;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrgInviteToken() {
    }

    public OrgInviteToken(Long orgId, String email, OrgRole role, String tokenHash, Instant expiresAt) {
        this.orgId = orgId;
        this.email = email;
        this.role = role;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.used = false;
        this.createdAt = Instant.now();
    }

    public boolean isUsable() {
        return !used && Instant.now().isBefore(expiresAt);
    }

    public void markUsed() {
        this.used = true;
    }
}
