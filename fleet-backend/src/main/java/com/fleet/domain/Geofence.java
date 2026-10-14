package com.fleet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "geofences", indexes = @Index(name = "idx_geofences_company", columnList = "company_id"))
public class Geofence extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    public Long companyId;

    @Column(nullable = false)
    public String name;

    @Enumerated(EnumType.STRING)
    public GeofenceType type = GeofenceType.CIRCLE;

    @Column(name = "center_lat")
    public Double centerLat;

    @Column(name = "center_lng")
    public Double centerLng;

    @Column(name = "radius_m")
    public Double radiusM;

    @Column(name = "polygon_json", columnDefinition = "TEXT")
    public String polygonJson;

    public String color = "#2563eb";

    public boolean active = true;

    @Column(name = "alert_on_enter")
    public boolean alertOnEnter = true;

    @Column(name = "alert_on_exit")
    public boolean alertOnExit = true;
}
