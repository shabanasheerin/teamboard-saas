package com.teamboard.repository;

import com.teamboard.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId, Pageable pageable);
}
