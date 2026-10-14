package com.fleet.web.dto;

import java.time.Instant;

import com.fleet.domain.LocationConfidence;

/** Standard GPS/GNSS telemetry payload pushed by a device or the ingestion API. */
public record TrackingPayload(Long vehicleId, String vehicleCode, Double latitude, Double longitude,
                             Double speedKmh, Integer heading, Boolean ignition, Instant timestamp,
                             LocationConfidence confidence) {
}
