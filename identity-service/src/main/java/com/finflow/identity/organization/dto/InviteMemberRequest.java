package com.finflow.identity.organization.dto;

import com.finflow.identity.organization.domain.OrgRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Body for {@code POST /api/v1/orgs/{orgId}/invites} — invite an existing user into the org. */
public record InviteMemberRequest(@NotBlank @Email String email, @NotNull OrgRole role) {
}
