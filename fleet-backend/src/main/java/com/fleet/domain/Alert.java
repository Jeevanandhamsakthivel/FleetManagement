package com.fleet.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alerts_company", columnList = "company_id"),
        @Index(name = "idx_alerts_vehicle", columnList = "vehicle_id"),
        @Index(name = "idx_alerts_created", columnList = "created_at")
})
public class Alert extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    public Long companyId;

    @Column(name = "vehicle_id")
    public Long vehicleId;

    @Column(name = "driver_id")
    public Long driverId;

    @Column(name = "geofence_id")
    public Long geofenceId;

    @Enumerated(EnumType.STRING)
    public AlertType type;

    @Enumerated(EnumType.STRING)
    public AlertSeverity severity = AlertSeverity.MEDIUM;

    public String message;

    public Double lat;
    public Double lng;

    @Column(name = "speed_kmph")
    public Double speedKmh;

    public boolean acknowledged = false;

    @Column(name = "acknowledged_at")
    public Instant acknowledgedAt;

    @Column(name = "acknowledged_by")
    public Long acknowledgedBy;
}
