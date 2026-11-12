package com.finflow.identity.organization.application;

import com.finflow.identity.organization.audit.application.AuditService;
import com.finflow.identity.organization.audit.dto.AuditLogResponse;
import com.finflow.identity.organization.dto.InviteMemberResponse;
import com.finflow.identity.organization.dto.OrgMemberResponse;
import com.finflow.identity.organization.dto.OrgResponse;
import com.finflow.identity.organization.domain.Membership;
import com.finflow.identity.organization.domain.MembershipStatus;
import com.finflow.identity.organization.domain.Org;
import com.finflow.identity.organization.domain.OrgInviteToken;
import com.finflow.identity.organization.domain.OrgRole;
import com.finflow.identity.auth.domain.User;
import com.finflow.identity.exception.ApiException;
import com.finflow.identity.integration.mail.MailService;
import com.finflow.identity.organization.persistence.MembershipRepository;
import com.finflow.identity.organization.persistence.OrgInviteTokenRepository;
import com.finflow.identity.organization.persistence.OrgRepository;
import com.finflow.identity.auth.persistence.UserRepository;
import com.finflow.identity.security.AuthorizationProperties;
import com.finflow.identity.security.TokenCrypto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OrgService {

    private final OrgRepository orgRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final OrgInviteTokenRepository orgInviteTokenRepository;
    private final MailService mailService;
    private final AuditService auditService;
    private final AuthorizationProperties authorizationProperties;
    private final long inviteExpirationMs;

    public OrgService(
            OrgRepository orgRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            OrgInviteTokenRepository orgInviteTokenRepository,
            MailService mailService,
            AuditService auditService,
            AuthorizationProperties authorizationProperties,
            @Value("${invite-expiration-ms}") long inviteExpirationMs
    ) {
        this.orgRepository = orgRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.orgInviteTokenRepository = orgInviteTokenRepository;
        this.mailService = mailService;
        this.auditService = auditService;
        this.authorizationProperties = authorizationProperties;
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

        List<Long> orgIds = memberships.stream()
                .map(Membership::getOrgId)
                .distinct()
                .toList();

        Map<Long, Org> organisationsById = new HashMap<>();
        for (Org organisation : orgRepository.findAllById(orgIds)) {
            organisationsById.put(organisation.getId(), organisation);
        }

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
     * Invite an existing user by email.
     * Previous outstanding invitations for the same organization and normalized email are invalidated before the replacement is issued.
     */
    @Transactional
    public InviteMemberResponse inviteMember(Long actorUserId, Long orgId, String email, OrgRole role) {
        lockOrg(orgId);
        requirePermission(actorUserId, orgId, "members:manage");

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> ApiException.notFound("User not found for email: " + normalizedEmail));

        if (role == null) {
            throw new IllegalArgumentException("Invitation role is required");
        }

        // Bulk update clears the persistence context.
        // Do this before loading the membership we intend to modify.
        orgInviteTokenRepository.invalidateOutstanding(orgId, normalizedEmail);
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
        orgInviteTokenRepository.save(new OrgInviteToken(orgId, normalizedEmail, role, TokenCrypto.sha256(rawInviteToken), Instant.now().plusMillis(inviteExpirationMs)));
        mailService.send(normalizedEmail,
                "FinFlow org invite",
                """
                You were invited to a FinFlow organisation.

                Sign in as this email, then paste the invite token into
                Organisations → Accept Invite (or the invites/accept API):

                %s

                If you were not expecting this, ignore this message.
                """.formatted(rawInviteToken).stripIndent().trim()
        );

        auditService.record(actorUserId, orgId, "MEMBER_INVITED", "MEMBERSHIP", String.valueOf(user.getId()), "role=" + role);
        return new InviteMemberResponse(orgId, normalizedEmail, role, "Invite sent", mailService.isEnabled() ? null : rawInviteToken);
    }

    /**
     * Accept an invitation using an atomically consumed one-time token.
     * The organization lock serializes acceptance against invitation replacement, role changes and membership removal.
     */
    @Transactional
    public OrgMemberResponse acceptInvite(Long actorUserId, String rawInviteToken) {
        String tokenHash = TokenCrypto.sha256(rawInviteToken);

        OrgInviteToken initialToken = orgInviteTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_TOKEN", "Invalid or expired invite token"));

        // The token provides the organization ID needed to acquire the lock.
        Long orgId = initialToken.getOrgId();
        lockOrg(orgId);
        // Re-read after obtaining the lock. The invitation may have been invalidated while this transaction was waiting for the lock.
        OrgInviteToken token = orgInviteTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_TOKEN", "Invalid or expired invite token"));

        String invitedEmail = token.getEmail();
        OrgRole invitedRole = token.getRole();

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + actorUserId));

        if (!actor.getEmail().equalsIgnoreCase(invitedEmail)) {
            throw ApiException.forbidden("Invite email does not match the logged-in user");
        }

        int consumed = orgInviteTokenRepository.consumeIfUsable(tokenHash, Instant.now());

        if (consumed != 1) {
            throw ApiException.unauthorized("INVALID_TOKEN", "Invalid or expired invite token");
        }

        // consumeIfUsable clears the persistence context, so load the
        // membership only after consuming the token.
        Membership membership = membershipRepository.findByUserIdAndOrgId(actorUserId, orgId)
                .orElseThrow(() -> ApiException.notFound("Invite membership not found"));

        // An old invitation must never change the role of an active member.
        if (!membership.isInvited()) {
            throw new IllegalArgumentException("Membership is not in an invited state");
        }

        membership.changeRole(invitedRole);
        membership.acceptInvite();
        auditService.record(actorUserId, orgId, "MEMBER_JOINED", "MEMBERSHIP", String.valueOf(actorUserId), "role=" + invitedRole);
        return toOrgMemberResponse(actor, membership);
    }

    @Transactional(readOnly = true)
    public List<OrgMemberResponse> listMembers(Long actorUserId, Long orgId) {
        requireActiveMembership(actorUserId, orgId);
        List<Membership> memberships = new ArrayList<>(membershipRepository.findByOrgIdAndStatus(orgId, MembershipStatus.ACTIVE));
        memberships.addAll(membershipRepository.findByOrgIdAndStatus(orgId, MembershipStatus.INVITED));

        if (memberships.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = memberships.stream()
                .map(Membership::getUserId)
                .distinct()
                .toList();

        Map<Long, User> usersById = new HashMap<>();
        for (User user : userRepository.findAllById(userIds)) {
            usersById.put(user.getId(), user);
        }

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
        lockOrg(orgId);
        requirePermission(actorUserId, orgId, "members:manage");

        if (role == null) {
            throw new IllegalArgumentException("Membership role is required");
        }

        Membership membership = requireActiveMembership(targetUserId, orgId);
        OrgRole previousRole = membership.getRole();
        if (previousRole == OrgRole.ORG_ADMIN && role != OrgRole.ORG_ADMIN) {
            ensureAnotherAdminRemains(orgId);
        }
        membership.changeRole(role);
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> ApiException.notFound("User not found: " + targetUserId));

        auditService.record(actorUserId, orgId, "ROLE_CHANGED", "MEMBERSHIP", String.valueOf(targetUserId), previousRole + "→" + role);
        return toOrgMemberResponse(user, membership);
    }

    @Transactional
    public void removeMember(Long actorUserId, Long orgId, Long targetUserId) {
        lockOrg(orgId);
        requirePermission(actorUserId, orgId, "members:manage");
        Membership membership = requireActiveMembership(targetUserId, orgId);
        if (membership.getRole() == OrgRole.ORG_ADMIN) {
            ensureAnotherAdminRemains(orgId);
        }
        membership.remove();
        auditService.record(actorUserId, orgId, "MEMBER_REMOVED", "MEMBERSHIP", String.valueOf(targetUserId), null);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listAudit(Long actorUserId, Long orgId) {
        requirePermission(actorUserId, orgId, "org:manage");
        return auditService.listForOrg(orgId);
    }

    /**
     * Acquire the organization row lock before changing its memberships.
     * The lock remains held until the surrounding transaction completes.
     */
    private Org lockOrg(Long orgId) {
        return orgRepository.findByIdForUpdate(orgId)
                .orElseThrow(() -> ApiException.notFound("Org not found: " + orgId));
    }

    private void ensureAnotherAdminRemains(Long orgId) {
        long adminCount = membershipRepository.countByOrgIdAndStatusAndRole(
                orgId,
                MembershipStatus.ACTIVE,
                OrgRole.ORG_ADMIN
        );

        if (adminCount <= 1) {
            throw new IllegalArgumentException(
                    "Cannot remove or demote the last ORG_ADMIN"
            );
        }
    }

    private void requirePermission(Long userId, Long orgId, String permission) {
        Membership membership = requireActiveMembership(userId, orgId);
        if (!authorizationProperties.roleGrants(membership.getRole(), permission)) {
            throw ApiException.forbidden("Missing permission: " + permission);
        }
    }

    private Membership requireActiveMembership(Long userId, Long orgId) {
        Membership membership = membershipRepository
                .findByUserIdAndOrgId(userId, orgId)
                .filter(Membership::isActive)
                .orElseThrow(() -> ApiException.forbidden("Not a member of org: " + orgId));

        if (!orgRepository.existsById(orgId)) {
            throw ApiException.notFound("Org not found: " + orgId);
        }
        return membership;
    }

    /**
     * Build the API DTO explicitly instead of exposing a managed JPA entity.
     */
    private static OrgResponse toOrgResponse(Org organisation, OrgRole callerRole) {
        return new OrgResponse(organisation.getId(), organisation.getName(), callerRole, organisation.getCreatedAt());
    }

    private static OrgMemberResponse toOrgMemberResponse(User user, Membership membership) {
        return new OrgMemberResponse(user.getId(), user.getEmail(), membership.getRole(), membership.getStatus());
    }
}
