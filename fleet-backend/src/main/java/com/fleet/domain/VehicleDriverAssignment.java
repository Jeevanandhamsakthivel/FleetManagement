package com.fleet.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicle_driver_assignments",
        indexes = @Index(name = "idx_vda_vehicle", columnList = "vehicle_id"))
public class VehicleDriverAssignment extends BaseEntity {

    @Column(name = "company_id")
    public Long companyId;

    @Column(name = "vehicle_id", nullable = false)
    public Long vehicleId;

    @Column(name = "driver_id", nullable = false)
    public Long driverId;

    @Column(name = "assigned_at")
    public Instant assignedAt = Instant.now();

    @Column(name = "released_at")
    public Instant releasedAt;

    public boolean active = true;
}
