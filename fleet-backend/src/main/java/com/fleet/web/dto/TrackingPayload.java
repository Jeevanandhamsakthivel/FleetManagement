package com.fleet.web.dto;

import java.time.Instant;

import com.fleet.domain.LocationConfidence;
import com.fleet.domain.LocationSource;

/** Standard GPS/GNSS telemetry payload pushed by a device, mobile browser or the ingestion API. */
public record TrackingPayload(Long vehicleId, String vehicleCode, Double latitude, Double longitude,
                             Double speedKmh, Integer heading, Boolean ignition, Instant timestamp,
                             LocationConfidence confidence, Double accuracy, LocationSource source) {
}
