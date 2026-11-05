package com.finflow.identity.dto.response;

import com.finflow.identity.entity.OrgRole;

import java.time.Instant;

/** Organisation summary including the caller's role in that org. */
public record OrgResponse(
        Long orgId,
        String name,
        OrgRole myRole,
        Instant createdAt
) {
}
