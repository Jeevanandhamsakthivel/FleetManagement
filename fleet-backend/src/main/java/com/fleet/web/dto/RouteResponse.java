package com.fleet.web.dto;

import java.util.List;

/**
 * Planned road route for a trip, including the corridor configuration and current
 * route state. {@code geometry} is a list of [lat,lng] pairs (never a fake straight line).
 */
public record RouteResponse(
        Long tripId,
        Long vehicleId,
        Long driverId,
        String origin,
        String destination,
        Double originLat,
        Double originLng,
        Double destLat,
        Double destLng,
        String provider,
        Double distanceKm,
        Integer corridorM,
        Integer confirmations,
        String state,
        Double lastDeviationM,
        List<double[]> geometry) {
}
