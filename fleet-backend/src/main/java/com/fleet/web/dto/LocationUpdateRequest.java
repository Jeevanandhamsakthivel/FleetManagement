package com.fleet.web.dto;

import java.time.Instant;

/**
 * A location reading pushed from a real device (typically a mobile browser using the
 * Geolocation API). Latitude/longitude are required; the rest are optional telemetry.
 */
public record LocationUpdateRequest(Double latitude, Double longitude, Double accuracy,
                                    Double speedKmh, Integer heading, Instant timestamp) {
}
