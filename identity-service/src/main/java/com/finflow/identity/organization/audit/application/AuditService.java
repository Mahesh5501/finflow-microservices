package com.finflow.identity.organization.audit.application;

import com.finflow.identity.organization.audit.dto.AuditLogResponse;
import com.finflow.identity.organization.audit.domain.AuditLog;
import com.finflow.identity.organization.audit.persistence.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Records identity actions and maps stored rows to audit responses; no compliance guarantee is implied. */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(Long actorUserId, Long orgId, String action, String resourceType, String resourceId, String details) {
        auditLogRepository.save(new AuditLog(actorUserId, orgId, action, resourceType, resourceId, details));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listForOrg(Long orgId) {
        return auditLogRepository.findByOrgIdOrderByCreatedAtDesc(orgId).stream()
                .map(entry -> new AuditLogResponse(
                        entry.getId(),
                        entry.getActorUserId(),
                        entry.getOrgId(),
                        entry.getAction(),
                        entry.getResourceType(),
                        entry.getResourceId(),
                        entry.getDetails(),
                        entry.getCreatedAt()
                ))
                .toList();
    }
}
