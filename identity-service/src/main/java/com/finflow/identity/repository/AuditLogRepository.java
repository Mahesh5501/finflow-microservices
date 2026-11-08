package com.finflow.identity.repository;

import com.finflow.identity.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByOrgIdOrderByCreatedAtDesc(Long orgId);
}
