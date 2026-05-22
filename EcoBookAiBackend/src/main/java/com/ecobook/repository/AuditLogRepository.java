package com.ecobook.repository;

import com.ecobook.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findByActorUserIdOrTargetUserIdOrderByCreatedAtDesc(UUID actorUserId, UUID targetUserId);
}
