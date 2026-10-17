package com.fleet.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
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

    // --- Planned road route (real routing geometry, never a fake straight line) ---

    @Column(name = "origin_lat")
    public Double originLat;

    @Column(name = "origin_lng")
    public Double originLng;

    @Column(name = "dest_lat")
    public Double destLat;

    @Column(name = "dest_lng")
    public Double destLng;

    /** JSON array of [lat,lng] pairs describing the planned road route. */
    @Lob
    @Column(name = "route_geometry", columnDefinition = "TEXT")
    public String routeGeometry;

    /** Routing provider that produced the geometry (e.g. "OSRM"). */
    @Column(name = "route_provider")
    public String routeProvider;

    @Column(name = "route_distance_km")
    public Double routeDistanceKm;

    // --- Route corridor + deviation monitoring ---

    /** Permitted distance (metres) the vehicle may stray from the planned route. */
    @Column(name = "deviation_threshold_m")
    public Integer deviationThresholdM = 500;

    /** Consecutive out-of-corridor readings required before a deviation alert fires. */
    @Column(name = "deviation_confirmations")
    public Integer deviationConfirmations = 3;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_state")
    public RouteState routeState;

    @Column(name = "last_deviation_at")
    public Instant lastDeviationAt;

    @Column(name = "last_deviation_m")
    public Double lastDeviationM;

    /** Consecutive out-of-corridor readings counted so far (persisted so restarts keep the streak). */
    @Column(name = "deviation_streak")
    public Integer deviationStreak = 0;

    /** Time of the most recent valid GPS reading associated with this trip. */
    @Column(name = "last_gps_at")
    public Instant lastGpsAt;
}
