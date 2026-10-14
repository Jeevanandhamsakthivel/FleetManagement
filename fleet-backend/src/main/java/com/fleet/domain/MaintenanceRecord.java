package com.fleet.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "maintenance_records",
        indexes = @Index(name = "idx_maintenance_vehicle", columnList = "vehicle_id"))
public class MaintenanceRecord extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    public Long companyId;

    @Column(name = "vehicle_id", nullable = false)
    public Long vehicleId;

    @Enumerated(EnumType.STRING)
    public MaintenanceType type = MaintenanceType.GENERAL;

    public String status = "OPEN";

    @Column(length = 1000)
    public String description;

    public String vendor;

    @Column(name = "service_date")
    public Instant serviceDate;

    @Column(name = "next_service_due")
    public Instant nextServiceDue;

    public Double cost = 0.0;

    public Double odometer;

    @Column(name = "work_order_number")
    public String workOrderNumber;
}
