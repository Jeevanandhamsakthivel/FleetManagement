package com.fleet.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "documents", indexes = @Index(name = "idx_documents_vehicle", columnList = "vehicle_id"))
public class VehicleDocument extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    public Long companyId;

    @Column(name = "vehicle_id")
    public Long vehicleId;

    @Column(name = "driver_id")
    public Long driverId;

    public String type;
    public String number;

    @Column(name = "issued_date")
    public LocalDate issuedDate;

    @Column(name = "expiry_date")
    public LocalDate expiryDate;

    @Column(name = "file_url")
    public String fileUrl;

    @Column(name = "expiry_alert_sent")
    public boolean expiryAlertSent = false;
}
