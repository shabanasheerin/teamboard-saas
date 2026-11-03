package com.teamboard.service;

import com.teamboard.dto.Dtos.AuditResponse;
import com.teamboard.dto.Dtos.PageResponse;
import com.teamboard.entity.AuditLog;
import com.teamboard.repository.AuditLogRepository;
import com.teamboard.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void log(AuthUser actor, String entityType, Long entityId, String action, String details) {
        AuditLog entry = new AuditLog();
        entry.setOrganizationId(actor.orgId());
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setDetails(details != null && details.length() > 500 ? details.substring(0, 500) : details);
        entry.setActorId(actor.id());
        entry.setActorName(actor.name());
        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditResponse> list(AuthUser me, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AuditLog> result = repository.findByOrganizationIdOrderByCreatedAtDesc(
                me.orgId(), PageRequest.of(Math.max(page, 0), safeSize));
        return new PageResponse<>(result.getContent().stream().map(AuditResponse::from).toList(),
                result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }
}
