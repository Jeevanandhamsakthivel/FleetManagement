package com.fleet.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "drivers", indexes = @Index(name = "idx_drivers_company", columnList = "company_id"))
public class Driver extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    public Long companyId;

    @Column(name = "user_id")
    public Long userId;

    @Column(name = "full_name", nullable = false)
    public String fullName;

    public String phone;
    public String email;

    @Column(name = "license_number")
    public String licenseNumber;

    @Column(name = "license_expiry")
    public LocalDate licenseExpiry;

    @Enumerated(EnumType.STRING)
    public DriverStatus status = DriverStatus.AVAILABLE;

    @Column(name = "safety_score")
    public Integer safetyScore = 100;
}
