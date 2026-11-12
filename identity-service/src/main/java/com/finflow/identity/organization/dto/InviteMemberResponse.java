package com.finflow.identity.organization.dto;

import com.finflow.identity.organization.domain.OrgRole;

/**
 * Result of inviting a member.
 * Membership is {@code INVITED} until they call accept-invite with {@code token}.
 */
public record InviteMemberResponse(
        Long orgId,
        String email,
        OrgRole role,
        String message,
        String token
) {
}
