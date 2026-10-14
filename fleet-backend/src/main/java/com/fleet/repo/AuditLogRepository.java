package com.fleet.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<AuditLog> findTop200ByOrderByCreatedAtDesc();
}
