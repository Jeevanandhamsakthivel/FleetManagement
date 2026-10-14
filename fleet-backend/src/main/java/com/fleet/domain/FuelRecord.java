package com.fleet.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "fuel_records", indexes = @Index(name = "idx_fuel_vehicle", columnList = "vehicle_id"))
public class FuelRecord extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    public Long companyId;

    @Column(name = "vehicle_id", nullable = false)
    public Long vehicleId;

    @Column(name = "driver_id")
    public Long driverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type")
    public FuelType fuelType = FuelType.DIESEL;

    /** Litres for fuel, kWh for electric. */
    public Double quantity = 0.0;

    public Double cost = 0.0;

    public Double odometer;

    public String station;

    @Column(name = "filled_at")
    public Instant filledAt = Instant.now();
}
