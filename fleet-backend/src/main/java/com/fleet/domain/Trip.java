package com.fleet.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "trips", indexes = {
        @Index(name = "idx_trips_company", columnList = "company_id"),
        @Index(name = "idx_trips_vehicle", columnList = "vehicle_id"),
        @Index(name = "idx_trips_driver", columnList = "driver_id"),
        @Index(name = "idx_trips_status", columnList = "status")
})
public class Trip extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    public Long companyId;

    @Column(name = "vehicle_id")
    public Long vehicleId;

    @Column(name = "driver_id")
    public Long driverId;

    public String origin;
    public String destination;

    @Enumerated(EnumType.STRING)
    public TripStatus status = TripStatus.PLANNED;

    @Column(name = "planned_start")
    public Instant plannedStart;

    @Column(name = "actual_start")
    public Instant actualStart;

    @Column(name = "actual_end")
    public Instant actualEnd;

    @Column(name = "planned_distance_km")
    public Double plannedDistanceKm;

    @Column(name = "distance_km")
    public Double distanceKm = 0.0;

    @Column(name = "start_odometer")
    public Double startOdometer;

    @Column(name = "end_odometer")
    public Double endOdometer;

    public String notes;
}
