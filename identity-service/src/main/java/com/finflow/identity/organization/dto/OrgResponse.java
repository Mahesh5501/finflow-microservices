package com.finflow.identity.organization.dto;

import com.finflow.identity.organization.domain.OrgRole;

import java.time.Instant;

/** Organisation summary including the caller's role in that org. */
public record OrgResponse(
        Long orgId,
        String name,
        OrgRole myRole,
        Instant createdAt
) {
}
