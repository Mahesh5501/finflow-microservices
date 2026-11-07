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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Coordinates organisation use cases inside service-level transactions.
 * Controllers own HTTP mapping; repositories own queries; entities own state transitions.
 * Constructor injection keeps these collaborators explicit without a generic manager layer.
 */
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
        return toOrgResponse(org, OrgRole.ORG_ADMIN);
    }

    @Transactional(readOnly = true)
    public List<OrgResponse> listMine(Long userId) {
        List<Membership> memberships = membershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE);
        if (memberships.isEmpty()) {
            return List.of();
        }

        // Fetch the referenced organisations together instead of a query for every membership (N+1).
        List<Long> orgIds = memberships.stream().map(Membership::getOrgId).distinct().toList();
        Map<Long, Org> organisationsById = new HashMap<>();
        for (Org organisation : orgRepository.findAllById(orgIds)) {
            organisationsById.put(organisation.getId(), organisation);
        }

        // Batch queries do not promise input order. Iterate memberships to preserve the existing response order.
        return memberships.stream()
                .map(membership -> {
                    Org organisation = organisationsById.get(membership.getOrgId());
                    if (organisation == null) {
                        throw ApiException.notFound("Org not found: " + membership.getOrgId());
                    }
                    return toOrgResponse(organisation, membership.getRole());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OrgResponse get(Long actorUserId, Long orgId) {
        Membership callerMembership = requireActiveMembership(actorUserId, orgId);
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> ApiException.notFound("Org not found: " + orgId));
        return toOrgResponse(org, callerMembership.getRole());
    }

    /**
     * Invite existing user by email → Membership INVITED + invite token.
     * Accept via {@link #acceptInvite}.
     */
    @Transactional
    public InviteMemberResponse inviteMember(Long actorUserId, Long orgId, String email, OrgRole role) {
        requirePermission(actorUserId, orgId, "members:manage");
        // Locale.ROOT keeps email matching stable when the server's default locale changes.
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> ApiException.notFound("User not found for email: " + normalizedEmail));

        Membership existingMembership = membershipRepository.findByUserIdAndOrgId(user.getId(), orgId).orElse(null);
        if (existingMembership != null && existingMembership.isActive()) {
            throw new IllegalArgumentException("User is already an active member of this org");
        }
        if (existingMembership != null) {
            existingMembership.markInvited(role);
        } else {
            membershipRepository.save(Membership.invited(user.getId(), orgId, role));
        }

        String rawInviteToken = TokenCrypto.newRawToken();
        orgInviteTokenRepository.save(new OrgInviteToken(
                orgId, normalizedEmail, role, TokenCrypto.sha256(rawInviteToken), Instant.now().plusMillis(inviteExpirationMs)
        ));
        mailService.send(normalizedEmail, "FinFlow org invite", "Invite token: " + rawInviteToken);
        auditService.record(
                actorUserId, orgId, "MEMBER_INVITED", "MEMBERSHIP",
                String.valueOf(user.getId()), "role=" + role
        );
        return new InviteMemberResponse(
                orgId,
                normalizedEmail,
                role,
                "Invite sent",
                mailService.isEnabled() ? null : rawInviteToken
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
        // Authorize before loading other users; batching must not widen data access.
        requireActiveMembership(actorUserId, orgId);
        List<Membership> memberships = new ArrayList<>(
                membershipRepository.findByOrgIdAndStatus(orgId, MembershipStatus.ACTIVE));
        memberships.addAll(membershipRepository.findByOrgIdAndStatus(orgId, MembershipStatus.INVITED));
        if (memberships.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = memberships.stream().map(Membership::getUserId).distinct().toList();
        Map<Long, User> usersById = new HashMap<>();
        for (User user : userRepository.findAllById(userIds)) {
            usersById.put(user.getId(), user);
        }

        // HashMap gives direct ID lookups; the membership list preserves ACTIVE-before-INVITED order.
        List<OrgMemberResponse> members = new ArrayList<>(memberships.size());
        for (Membership membership : memberships) {
            User user = usersById.get(membership.getUserId());
            if (user == null) {
                throw ApiException.notFound("User not found: " + membership.getUserId());
            }
            members.add(toOrgMemberResponse(user, membership));
        }
        return members;
    }

    @Transactional
    public OrgMemberResponse changeRole(Long actorUserId, Long orgId, Long targetUserId, OrgRole role) {
        requirePermission(actorUserId, orgId, "members:manage");
        Membership membership = requireActiveMembership(targetUserId, orgId);
        OrgRole previousRole = membership.getRole();
        if (previousRole == OrgRole.ORG_ADMIN && role != OrgRole.ORG_ADMIN) {
            ensureAnotherAdminRemains(orgId);
        }
        membership.changeRole(role);
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + targetUserId));
        auditService.record(
                actorUserId, orgId, "ROLE_CHANGED", "MEMBERSHIP",
                String.valueOf(targetUserId), previousRole + "→" + role
        );
        return toOrgMemberResponse(user, membership);
    }

    @Transactional
    public void removeMember(Long actorUserId, Long orgId, Long targetUserId) {
        requirePermission(actorUserId, orgId, "members:manage");
        Membership membership = requireActiveMembership(targetUserId, orgId);
        if (membership.getRole() == OrgRole.ORG_ADMIN) {
            ensureAnotherAdminRemains(orgId);
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

    private void ensureAnotherAdminRemains(Long orgId) {
        // Count in SQL without materialising every active member.
        // This remains a check-then-write guard; concurrent removals require a separate locking design.
        long adminCount = membershipRepository.countByOrgIdAndStatusAndRole(
                orgId, MembershipStatus.ACTIVE, OrgRole.ORG_ADMIN);
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

    /** Build the API DTO explicitly instead of exposing a managed JPA entity. */
    private static OrgResponse toOrgResponse(Org organisation, OrgRole callerRole) {
        return new OrgResponse(organisation.getId(), organisation.getName(), callerRole, organisation.getCreatedAt());
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
