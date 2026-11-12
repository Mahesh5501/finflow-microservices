package com.finflow.identity.organization.dto;

import com.finflow.identity.organization.domain.MembershipStatus;
import com.finflow.identity.organization.domain.OrgRole;

/** One organisation member in list/add/role-update responses. */
public record OrgMemberResponse(
        Long userId,
        String email,
        OrgRole role,
        MembershipStatus status
) {
}
