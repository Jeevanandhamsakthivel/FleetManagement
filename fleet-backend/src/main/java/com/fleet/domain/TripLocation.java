package com.fleet.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "trip_locations", indexes = {
        @Index(name = "idx_trip_locations_trip", columnList = "trip_id"),
        @Index(name = "idx_trip_locations_vehicle", columnList = "vehicle_id"),
        @Index(name = "idx_trip_locations_recorded", columnList = "recorded_at")
})
public class TripLocation extends BaseEntity {

    @Column(name = "trip_id")
    public Long tripId;

    @Column(name = "vehicle_id", nullable = false)
    public Long vehicleId;

    @Column(name = "lat", nullable = false)
    public Double latitude;

    @Column(name = "lng", nullable = false)
    public Double longitude;

    @Column(name = "speed_kmph")
    public Double speedKmh = 0.0;

    public Integer heading = 0;

    public Boolean ignition = false;

    @Enumerated(EnumType.STRING)
    public LocationConfidence confidence = LocationConfidence.HIGH;

    @Column(name = "recorded_at")
    public Instant recordedAt = Instant.now();
}
