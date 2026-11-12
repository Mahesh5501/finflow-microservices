package com.finflow.identity.organization.audit.dto;

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
