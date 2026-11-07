package com.finflow.identity.service;

import com.finflow.identity.dto.response.AuditLogResponse;
import com.finflow.identity.dto.response.InviteMemberResponse;
import com.finflow.identity.dto.response.OrgMemberResponse;
import com.finflow.identity.dto.response.OrgResponse;
import com.finflow.identity.entity.Membership;
import com.finflow.identity.entity.MembershipStatus;
import com.finflow.identity.entity.Org;
import com.finflow.identity.entity.OrgInviteToken;
import com.finflow.identity.entity.OrgRole;
import com.finflow.identity.entity.User;
import com.finflow.identity.exception.ApiException;
import com.finflow.identity.mail.MailService;
import com.finflow.identity.repository.MembershipRepository;
import com.finflow.identity.repository.OrgInviteTokenRepository;
import com.finflow.identity.repository.OrgRepository;
import com.finflow.identity.repository.UserRepository;
import com.finflow.identity.security.token.TokenCrypto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrgService {

    private final OrgRepository orgRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final OrgInviteTokenRepository orgInviteTokenRepository;
    private final MailService mailService;
    private final AuditService auditService;
    private final long inviteExpirationMs;

    public OrgService(
            OrgRepository orgRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            OrgInviteTokenRepository orgInviteTokenRepository,
            MailService mailService,
            AuditService auditService,
            @Value("${invite-expiration-ms}") long inviteExpirationMs
    ) {
        this.orgRepository = orgRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.orgInviteTokenRepository = orgInviteTokenRepository;
        this.mailService = mailService;
        this.auditService = auditService;
        this.inviteExpirationMs = inviteExpirationMs;
    }

    @Transactional
    public OrgResponse create(Long actorUserId, String name) {
        Org org = orgRepository.save(new Org(name.trim()));
        membershipRepository.save(new Membership(actorUserId, org.getId(), OrgRole.ORG_ADMIN));
        auditService.record(actorUserId, org.getId(), "ORG_CREATED", "ORG", String.valueOf(org.getId()), name);
        return new OrgResponse(org.getId(), org.getName(), OrgRole.ORG_ADMIN, org.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<OrgResponse> listMine(Long userId) {
        return membershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE).stream()
                .map(m -> {
                    Org org = orgRepository.findById(m.getOrgId())
                            .orElseThrow(() -> ApiException.notFound("Org not found: " + m.getOrgId()));
                    return new OrgResponse(org.getId(), org.getName(), m.getRole(), org.getCreatedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OrgResponse get(Long actorUserId, Long orgId) {
        Membership mine = requireActiveMembership(actorUserId, orgId);
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> ApiException.notFound("Org not found: " + orgId));
        return new OrgResponse(org.getId(), org.getName(), mine.getRole(), org.getCreatedAt());
    }

    /**
     * Invite existing user by email → Membership INVITED + invite token.
     * Accept via {@link #acceptInvite}.
     */
    @Transactional
    public InviteMemberResponse inviteMember(Long actorUserId, Long orgId, String email, OrgRole role) {
        requirePermission(actorUserId, orgId, "members:manage");
        String normalized = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> ApiException.notFound("User not found for email: " + normalized));

        Membership existing = membershipRepository.findByUserIdAndOrgId(user.getId(), orgId).orElse(null);
        if (existing != null && existing.isActive()) {
            throw new IllegalArgumentException("User is already an active member of this org");
        }
        if (existing != null) {
            existing.markInvited(role);
        } else {
            membershipRepository.save(Membership.invited(user.getId(), orgId, role));
        }

        String raw = TokenCrypto.newRawToken();
        orgInviteTokenRepository.save(new OrgInviteToken(
                orgId, normalized, role, TokenCrypto.sha256(raw), Instant.now().plusMillis(inviteExpirationMs)
        ));
        mailService.send(normalized, "FinFlow org invite", "Invite token: " + raw);
        auditService.record(
                actorUserId, orgId, "MEMBER_INVITED", "MEMBERSHIP",
                String.valueOf(user.getId()), "role=" + role
        );
        return new InviteMemberResponse(
                orgId,
                normalized,
                role,
                "Invite sent",
                mailService.isEnabled() ? null : raw
        );
    }

    @Transactional
    public OrgMemberResponse acceptInvite(Long actorUserId, String rawInviteToken) {
        OrgInviteToken token = orgInviteTokenRepository.findByTokenHash(TokenCrypto.sha256(rawInviteToken))
                .orElseThrow(() -> ApiException.unauthorized("INVALID_TOKEN", "Invalid or expired invite token"));
        if (!token.isUsable()) {
            throw ApiException.unauthorized("INVALID_TOKEN", "Invalid or expired invite token");
        }
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + actorUserId));
        if (!actor.getEmail().equalsIgnoreCase(token.getEmail())) {
            throw ApiException.forbidden("Invite email does not match the logged-in user");
        }

        Membership membership = membershipRepository.findByUserIdAndOrgId(actorUserId, token.getOrgId())
                .orElseThrow(() -> ApiException.notFound("Invite membership not found"));
        if (!membership.isInvited() && !membership.isActive()) {
            throw new IllegalArgumentException("Membership is not in an invitable state");
        }
        membership.changeRole(token.getRole());
        membership.acceptInvite();
        token.markUsed();
        auditService.record(
                actorUserId, token.getOrgId(), "MEMBER_JOINED", "MEMBERSHIP",
                String.valueOf(actorUserId), "role=" + token.getRole()
        );
        return toOrgMemberResponse(actor, membership);
    }

    @Transactional(readOnly = true)
    public List<OrgMemberResponse> listMembers(Long actorUserId, Long orgId) {
        requireActiveMembership(actorUserId, orgId);
        List<OrgMemberResponse> result = new ArrayList<>();
        for (MembershipStatus status : List.of(MembershipStatus.ACTIVE, MembershipStatus.INVITED)) {
            for (Membership m : membershipRepository.findByOrgIdAndStatus(orgId, status)) {
                User user = userRepository.findById(m.getUserId())
                        .orElseThrow(() -> ApiException.notFound("User not found: " + m.getUserId()));
                result.add(toOrgMemberResponse(user, m));
            }
        }
        return result;
    }

    @Transactional
    public OrgMemberResponse changeRole(Long actorUserId, Long orgId, Long targetUserId, OrgRole role) {
        requirePermission(actorUserId, orgId, "members:manage");
        Membership membership = requireActiveMembership(targetUserId, orgId);
        OrgRole previous = membership.getRole();
        if (previous == OrgRole.ORG_ADMIN && role != OrgRole.ORG_ADMIN) {
            ensureNotLastAdmin(orgId);
        }
        membership.changeRole(role);
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + targetUserId));
        auditService.record(
                actorUserId, orgId, "ROLE_CHANGED", "MEMBERSHIP",
                String.valueOf(targetUserId), previous + "→" + role
        );
        return toOrgMemberResponse(user, membership);
    }

    @Transactional
    public void removeMember(Long actorUserId, Long orgId, Long targetUserId) {
        requirePermission(actorUserId, orgId, "members:manage");
        Membership membership = requireActiveMembership(targetUserId, orgId);
        if (membership.getRole() == OrgRole.ORG_ADMIN) {
            ensureNotLastAdmin(orgId);
        }
        membership.remove();
        auditService.record(
                actorUserId, orgId, "MEMBER_REMOVED", "MEMBERSHIP",
                String.valueOf(targetUserId), null
        );
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listAudit(Long actorUserId, Long orgId) {
        requirePermission(actorUserId, orgId, "org:manage");
        return auditService.listForOrg(orgId);
    }

    private void ensureNotLastAdmin(Long orgId) {
        long adminCount = membershipRepository.findByOrgIdAndStatus(orgId, MembershipStatus.ACTIVE).stream()
                .filter(m -> m.getRole() == OrgRole.ORG_ADMIN)
                .count();
        if (adminCount <= 1) {
            throw new IllegalArgumentException("Cannot remove or demote the last ORG_ADMIN");
        }
    }

    private void requirePermission(Long userId, Long orgId, String permission) {
        Membership membership = requireActiveMembership(userId, orgId);
        if (!membership.getRole().permissions().contains(permission)) {
            throw ApiException.forbidden("Missing permission: " + permission);
        }
    }

    private Membership requireActiveMembership(Long userId, Long orgId) {
        Membership membership = membershipRepository.findByUserIdAndOrgId(userId, orgId)
                .filter(Membership::isActive)
                .orElseThrow(() -> ApiException.forbidden("Not a member of org: " + orgId));
        if (!orgRepository.existsById(orgId)) {
            throw ApiException.notFound("Org not found: " + orgId);
        }
        return membership;
    }

    private static OrgMemberResponse toOrgMemberResponse(User user, Membership membership) {
        return new OrgMemberResponse(
                user.getId(),
                user.getEmail(),
                membership.getRole(),
                membership.getStatus()
        );
    }
}
