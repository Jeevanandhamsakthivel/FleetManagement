package com.fleet.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fleet.domain.AuditLog;
import com.fleet.repo.AuditLogRepository;
import com.fleet.security.CurrentUser;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

    private final AuditLogRepository auditLogs;
    private final CurrentUser currentUser;

    public AuditController(AuditLogRepository auditLogs, CurrentUser currentUser) {
        this.auditLogs = auditLogs;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<AuditLog> list() {
        if ("SUPER_ADMIN".equals(currentUser.get() != null ? currentUser.get().role() : null)) {
            return auditLogs.findTop200ByOrderByCreatedAtDesc();
        }
        return auditLogs.findAllByCompanyIdOrderByCreatedAtDesc(currentUser.companyId());
    }
}
