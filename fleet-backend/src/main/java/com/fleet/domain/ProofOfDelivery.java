package com.fleet.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "proof_of_delivery",
        indexes = @Index(name = "idx_pod_delivery", columnList = "delivery_id"))
public class ProofOfDelivery extends BaseEntity {

    @Column(name = "delivery_id", nullable = false)
    public Long deliveryId;

    @Enumerated(EnumType.STRING)
    public PodType type = PodType.PHOTO;

    @Column(name = "photo_url")
    public String photoUrl;

    @Column(length = 2000)
    public String signature;

    public String otp;

    @Column(name = "recipient_name")
    public String recipientName;

    @Column(name = "captured_at")
    public Instant capturedAt = Instant.now();
}
