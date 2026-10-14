package com.fleet.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicles", indexes = {
        @Index(name = "idx_vehicles_company", columnList = "company_id"),
        @Index(name = "idx_vehicles_status", columnList = "status")
})
public class Vehicle extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    public Long companyId;

    @Column(nullable = false, unique = true)
    public String code;

    @Column(name = "registration_number")
    public String registrationNumber;

    public String type;
    public String make;
    public String model;

    @Column(name = "model_year")
    public Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type")
    public FuelType fuelType = FuelType.DIESEL;

    @Enumerated(EnumType.STRING)
    public VehicleStatus status = VehicleStatus.OFFLINE;

    public Double odometer = 0.0;

    @Column(name = "last_lat")
    public Double lastLat;

    @Column(name = "last_lng")
    public Double lastLng;

    @Column(name = "last_speed")
    public Double lastSpeed = 0.0;

    @Column(name = "last_heading")
    public Integer lastHeading = 0;

    @Column(name = "last_ignition")
    public Boolean lastIgnition = false;

    @Column(name = "last_update")
    public Instant lastUpdate;

    @Enumerated(EnumType.STRING)
    public LocationConfidence confidence;
}
