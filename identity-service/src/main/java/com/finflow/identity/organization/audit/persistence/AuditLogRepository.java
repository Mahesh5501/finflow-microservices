package com.finflow.identity.organization.audit.persistence;

import com.finflow.identity.organization.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByOrgIdOrderByCreatedAtDesc(Long orgId);
}
