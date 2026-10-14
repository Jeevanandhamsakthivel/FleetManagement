package com.fleet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_company", columnList = "company_id"),
        @Index(name = "idx_audit_entity", columnList = "entity")
})
public class AuditLog extends BaseEntity {

    @Column(name = "company_id")
    public Long companyId;

    @Column(name = "user_id")
    public Long userId;

    public String username;

    public String action;
    public String entity;

    @Column(name = "entity_id")
    public Long entityId;

    @Column(length = 2000)
    public String details;

    @Column(name = "ip_address")
    public String ipAddress;
}
