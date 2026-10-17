package com.fleet.domain;

/**
 * High-level position of a vehicle relative to its active trip's planned road route.
 * Used to drive the dashboard status (ON ROUTE / ROUTE DEVIATION / GPS SIGNAL LOST /
 * TRIP COMPLETED) without inferring it on the client.
 */
public enum RouteState {
    ON_ROUTE,
    DEVIATED,
    GPS_LOST,
    COMPLETED
}
