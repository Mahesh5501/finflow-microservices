package com.finflow.identity.service;

import com.finflow.identity.dto.response.AuditLogResponse;
import com.finflow.identity.entity.AuditLog;
import com.finflow.identity.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Writes / reads compliance audit entries. */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(
            Long actorUserId,
            Long orgId,
            String action,
            String resourceType,
            String resourceId,
            String details
    ) {
        auditLogRepository.save(new AuditLog(actorUserId, orgId, action, resourceType, resourceId, details));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listForOrg(Long orgId) {
        return auditLogRepository.findByOrgIdOrderByCreatedAtDesc(orgId).stream()
                .map(a -> new AuditLogResponse(
                        a.getId(),
                        a.getActorUserId(),
                        a.getOrgId(),
                        a.getAction(),
                        a.getResourceType(),
                        a.getResourceId(),
                        a.getDetails(),
                        a.getCreatedAt()
                ))
                .toList();
    }
}
