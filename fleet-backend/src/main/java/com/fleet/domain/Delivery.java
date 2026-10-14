package com.fleet.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "deliveries", indexes = {
        @Index(name = "idx_deliveries_company", columnList = "company_id"),
        @Index(name = "idx_deliveries_customer", columnList = "customer_id")
})
public class Delivery extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    public Long companyId;

    @Column(name = "customer_id")
    public Long customerId;

    @Column(name = "vehicle_id")
    public Long vehicleId;

    @Column(name = "driver_id")
    public Long driverId;

    public String reference;

    @Column(name = "pickup_address")
    public String pickupAddress;

    @Column(name = "delivery_address")
    public String deliveryAddress;

    @Enumerated(EnumType.STRING)
    public DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "scheduled_at")
    public Instant scheduledAt;

    @Column(name = "delivered_at")
    public Instant deliveredAt;

    @Column(name = "eta")
    public Instant eta;

    @Column(length = 1000)
    public String notes;

    @Column(name = "tracking_token", unique = true)
    public String trackingToken;
}
