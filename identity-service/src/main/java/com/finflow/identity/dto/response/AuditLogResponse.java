package com.finflow.identity.dto.response;

import java.time.Instant;

/** One audit row for org compliance views. */
public record AuditLogResponse(
        Long auditId,
        Long actorUserId,
        Long orgId,
        String action,
        String resourceType,
        String resourceId,
        String details,
        Instant createdAt
) {
}
