package com.finflow.identity.dto.request;

import com.finflow.identity.entity.OrgRole;
import jakarta.validation.constraints.NotNull;

/** Request body for {@code PUT /api/v1/orgs/{orgId}/members/{userId}/role}. */
public record UpdateOrgMemberRoleRequest(
        @NotNull OrgRole role
) {
}
