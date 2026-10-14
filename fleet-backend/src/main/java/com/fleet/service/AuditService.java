package com.fleet.service;

import org.springframework.stereotype.Service;

import com.fleet.domain.AuditLog;
import com.fleet.repo.AuditLogRepository;

@Service
public class AuditService {

    private final AuditLogRepository auditLogs;

    public AuditService(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    public void record(Long companyId, Long userId, String username, String action, String entity,
                       Long entityId, String details) {
        AuditLog log = new AuditLog();
        log.companyId = companyId;
        log.userId = userId;
        log.username = username;
        log.action = action;
        log.entity = entity;
        log.entityId = entityId;
        log.details = details;
        auditLogs.save(log);
    }
}
