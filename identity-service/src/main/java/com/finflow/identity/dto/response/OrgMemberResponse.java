package com.finflow.identity.dto.response;

import com.finflow.identity.entity.MembershipStatus;
import com.finflow.identity.entity.OrgRole;

/** One organisation member in list/add/role-update responses. */
public record OrgMemberResponse(
        Long userId,
        String email,
        OrgRole role,
        MembershipStatus status
) {
}
