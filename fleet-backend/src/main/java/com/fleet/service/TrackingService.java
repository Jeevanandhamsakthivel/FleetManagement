package com.fleet.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fleet.domain.Alert;
import com.fleet.domain.AlertSeverity;
import com.fleet.domain.AlertType;
import com.fleet.domain.Geofence;
import com.fleet.domain.LocationConfidence;
import com.fleet.domain.LocationSource;
import com.fleet.domain.RouteState;
import com.fleet.domain.Trip;
import com.fleet.domain.TripLocation;
import com.fleet.domain.TripStatus;
import com.fleet.domain.Vehicle;
import com.fleet.domain.VehicleStatus;
import com.fleet.repo.AlertRepository;
import com.fleet.repo.GeofenceRepository;
import com.fleet.repo.TripLocationRepository;
import com.fleet.repo.TripRepository;
import com.fleet.repo.VehicleDriverAssignmentRepository;
import com.fleet.repo.VehicleRepository;
import com.fleet.web.dto.TrackingPayload;
import com.fleet.ws.TrackingWebSocketHandler;

@Service
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);
    private static final double OVERSPEED_LIMIT = 80.0;

    private final VehicleRepository vehicles;
    private final TripRepository trips;
    private final TripLocationRepository locations;
    private final GeofenceRepository geofences;
    private final AlertRepository alerts;
    private final VehicleDriverAssignmentRepository assignments;
    private final TrackingWebSocketHandler ws;

    /** A trip with no valid GPS reading for this long is flagged GPS_SIGNAL_LOST. */
    @Value("${app.tracking.gps-lost-seconds:180}")
    private long gpsLostSeconds;

    public TrackingService(VehicleRepository vehicles, TripRepository trips, TripLocationRepository locations,
                           GeofenceRepository geofences, AlertRepository alerts,
                           VehicleDriverAssignmentRepository assignments, TrackingWebSocketHandler ws) {
        this.vehicles = vehicles;
        this.trips = trips;
        this.locations = locations;
        this.geofences = geofences;
        this.alerts = alerts;
        this.assignments = assignments;
        this.ws = ws;
    }

    @Transactional
    public TripLocation ingest(TrackingPayload p) {
        Vehicle v = resolveVehicle(p);
        if (v == null) {
            throw new IllegalArgumentException("Unknown vehicle");
        }
        Instant ts = p.timestamp() != null ? p.timestamp() : Instant.now();
        double lat = p.latitude() != null ? p.latitude() : nz(v.lastLat);
        double lng = p.longitude() != null ? p.longitude() : nz(v.lastLng);
        double speed = p.speedKmh() != null ? p.speedKmh() : 0.0;
        int heading = p.heading() != null ? p.heading() : (v.lastHeading != null ? v.lastHeading : 0);
        boolean ignition = p.ignition() != null ? p.ignition() : speed > 0;
        LocationSource source = p.source() != null ? p.source() : LocationSource.DEVICE;
        LocationConfidence confidence = p.confidence() != null ? p.confidence() : confidenceFromAccuracy(p.accuracy());

        // Out-of-order protection: a late/duplicate reading older than the latest known
        // position must never overwrite newer data. We still persist it for history/playback.
        boolean isLatest = v.lastUpdate == null || !ts.isBefore(v.lastUpdate);

        Double prevLat = v.lastLat;
        Double prevLng = v.lastLng;
        Double prevSpeed = v.lastSpeed;
        boolean hadPrev = prevLat != null && prevLng != null;

        Trip active = trips.findFirstByVehicleIdAndStatusOrderByIdDesc(v.id, TripStatus.IN_PROGRESS).orElse(null);

        TripLocation loc = new TripLocation();
        loc.vehicleId = v.id;
        loc.tripId = active != null ? active.id : null;
        loc.latitude = lat;
        loc.longitude = lng;
        loc.speedKmh = speed;
        loc.heading = heading;
        loc.ignition = ignition;
        loc.recordedAt = ts;
        loc.confidence = confidence;
        loc.source = source;
        loc.accuracy = p.accuracy();
        locations.save(loc);

        if (!isLatest) {
            // Historical record stored, but do not move the live marker backwards.
            return loc;
        }

        if (hadPrev) {
            double distKm = haversine(prevLat, prevLng, lat, lng);
            if (active != null) {
                active.distanceKm = nz(active.distanceKm) + distKm;
                trips.save(active);
            }
            v.odometer = nz(v.odometer) + distKm;
        }

        if (speed > 3.0) {
            v.status = VehicleStatus.MOVING;
        } else if (ignition) {
            v.status = VehicleStatus.IDLE;
        } else {
            v.status = VehicleStatus.STOPPED;
        }
        v.lastLat = lat;
        v.lastLng = lng;
        v.lastSpeed = speed;
        v.lastHeading = heading;
        v.lastIgnition = ignition;
        v.lastUpdate = ts;
        v.confidence = confidence;
        v.lastAccuracy = p.accuracy();
        v.lastSource = source;
        vehicles.save(v);

        Long driverId = active != null && active.driverId != null ? active.driverId
                : assignments.findFirstByVehicleIdAndActiveTrue(v.id).map(a -> a.driverId).orElse(null);

        if (speed > OVERSPEED_LIMIT && (prevSpeed == null || prevSpeed <= OVERSPEED_LIMIT)) {
            raiseAlert(v, driverId, null, AlertType.OVERSPEED, AlertSeverity.HIGH,
                    "Overspeed detected: " + Math.round(speed) + " km/h", lat, lng, speed);
        }

        if (hadPrev) {
            evaluateGeofences(v, prevLat, prevLng, lat, lng, driverId);
        }

        evaluateRoute(v, active, lat, lng, p.accuracy(), driverId, ts);

        broadcast(v, lat, lng, speed, heading, ts, source);
        return loc;
    }

    /**
     * Compares a valid GPS reading against the active trip's planned road route and manages
     * the ON ROUTE / ROUTE DEVIATION / GPS SIGNAL LOST state machine. A deviation alert only
     * fires after several consecutive out-of-corridor readings (configurable per trip) and
     * accounts for the reported GPS accuracy so a single coarse fix cannot raise a false alarm.
     */
    private void evaluateRoute(Vehicle v, Trip trip, double lat, double lng, Double accuracy,
                               Long driverId, Instant ts) {
        if (trip == null || trip.routeGeometry == null || trip.routeGeometry.isBlank()) {
            return;
        }
        List<double[]> route = RouteGeometry.fromJson(trip.routeGeometry);
        if (route.size() < 2) {
            return;
        }
        trip.lastGpsAt = ts;
        double distance = RouteGeometry.distanceToRouteM(route, lat, lng);
        trip.lastDeviationM = distance;

        int threshold = trip.deviationThresholdM != null ? trip.deviationThresholdM : 500;
        int confirmations = trip.deviationConfirmations != null ? trip.deviationConfirmations : 3;
        // Poor GPS fixes (large reported accuracy) get extra slack so noise is tolerated.
        double effective = threshold + Math.min(accuracy != null ? Math.max(0, accuracy) : 0, 150);
        boolean outside = distance > effective;
        RouteState prev = trip.routeState;

        if (outside) {
            trip.deviationStreak = (trip.deviationStreak != null ? trip.deviationStreak : 0) + 1;
            if (trip.deviationStreak >= confirmations && prev != RouteState.DEVIATED) {
                trip.routeState = RouteState.DEVIATED;
                trip.lastDeviationAt = ts;
                raiseAlert(v, driverId, null, AlertType.ROUTE_DEVIATION, AlertSeverity.HIGH,
                        v.code + " is " + Math.round(distance) + " m off the planned route", lat, lng, nz(v.lastSpeed));
                broadcastRouteState(v, trip, distance, threshold);
            }
        } else {
            trip.deviationStreak = 0;
            if (prev == RouteState.DEVIATED) {
                trip.routeState = RouteState.ON_ROUTE;
                acknowledge(v.id, AlertType.ROUTE_DEVIATION);
                broadcastRouteState(v, trip, distance, threshold);
            } else if (prev == RouteState.GPS_LOST) {
                trip.routeState = RouteState.ON_ROUTE;
                acknowledge(v.id, AlertType.GPS_SIGNAL_LOST);
                broadcastRouteState(v, trip, distance, threshold);
            } else if (prev == null || prev == RouteState.COMPLETED) {
                trip.routeState = RouteState.ON_ROUTE;
            }
        }
        trips.save(trip);
    }

    /** Auto-closes open alerts of a type once the vehicle recovers (returns to the corridor). */
    private void acknowledge(Long vehicleId, AlertType type) {
        List<Alert> open = alerts.findByVehicleIdAndTypeAndAcknowledgedFalse(vehicleId, type);
        for (Alert a : open) {
            a.acknowledged = true;
            a.acknowledgedAt = Instant.now();
            alerts.save(a);
        }
    }

    /**
     * Flags active trips whose device has gone quiet (no GPS for {@code gpsLostSeconds}),
     * so a stalled phone/GPS shows as GPS SIGNAL LOST instead of looking permanently on route.
     */
    @Scheduled(fixedRateString = "${app.tracking.gps-lost-check-ms:60000}")
    @Transactional
    public void checkGpsSignalLoss() {
        Instant now = Instant.now();
        for (Trip t : trips.findAllByStatus(TripStatus.IN_PROGRESS)) {
            if (t.routeGeometry == null || t.routeGeometry.isBlank() || t.routeState == RouteState.GPS_LOST) {
                continue;
            }
            Instant last = t.lastGpsAt != null ? t.lastGpsAt : t.actualStart;
            if (last == null || !now.isAfter(last.plusSeconds(gpsLostSeconds))) {
                continue;
            }
            t.routeState = RouteState.GPS_LOST;
            trips.save(t);
            if (t.vehicleId != null) {
                Vehicle v = vehicles.findById(t.vehicleId).orElse(null);
                if (v != null) {
                    raiseAlert(v, t.driverId, null, AlertType.GPS_SIGNAL_LOST, AlertSeverity.MEDIUM,
                            v.code + " GPS signal lost (no update for " + (gpsLostSeconds / 60) + " min)",
                            nz(v.lastLat), nz(v.lastLng), nz(v.lastSpeed));
                    broadcastRouteState(v, t, null, t.deviationThresholdM);
                }
            }
        }
    }

    private void broadcastRouteState(Vehicle v, Trip t, Double distanceM, Integer thresholdM) {
        String state = t.routeState != null ? t.routeState.name() : RouteState.ON_ROUTE.name();
        String json = String.format(
                "{\"type\":\"route-state\",\"vehicleId\":%d,\"tripId\":%s,\"state\":\"%s\","
                        + "\"distanceM\":%s,\"thresholdM\":%s,\"ts\":\"%s\"}",
                v.id, t.id == null ? "null" : t.id.toString(), state,
                distanceM == null ? "null" : String.format("%.1f", distanceM),
                thresholdM == null ? "null" : thresholdM.toString(), Instant.now());
        ws.broadcast(json, v.companyId);
    }

    /** Maps reported horizontal accuracy (metres) to a coarse confidence bucket. */
    private static LocationConfidence confidenceFromAccuracy(Double accuracy) {
        if (accuracy == null) {
            return LocationConfidence.HIGH;
        }
        if (accuracy <= 25) {
            return LocationConfidence.HIGH;
        }
        if (accuracy <= 100) {
            return LocationConfidence.MEDIUM;
        }
        return LocationConfidence.LOW;
    }

    private void evaluateGeofences(Vehicle v, double prevLat, double prevLng, double lat, double lng, Long driverId) {
        List<Geofence> fences = geofences.findAllByCompanyIdAndActiveTrue(v.companyId);
        for (Geofence f : fences) {
            boolean before = inside(f, prevLat, prevLng);
            boolean now = inside(f, lat, lng);
            if (!before && now && f.alertOnEnter) {
                raiseAlert(v, driverId, f.id, AlertType.GEOFENCE_ENTER, AlertSeverity.MEDIUM,
                        v.code + " entered geofence '" + f.name + "'", lat, lng, nz(v.lastSpeed));
            } else if (before && !now && f.alertOnExit) {
                raiseAlert(v, driverId, f.id, AlertType.GEOFENCE_EXIT, AlertSeverity.MEDIUM,
                        v.code + " exited geofence '" + f.name + "'", lat, lng, nz(v.lastSpeed));
            }
        }
    }

    public Alert raiseAlert(Vehicle v, Long driverId, Long geofenceId, AlertType type, AlertSeverity severity,
                            String message, double lat, double lng, double speed) {
        Alert a = new Alert();
        a.companyId = v.companyId;
        a.vehicleId = v.id;
        a.driverId = driverId;
        a.geofenceId = geofenceId;
        a.type = type;
        a.severity = severity;
        a.message = message;
        a.lat = lat;
        a.lng = lng;
        a.speedKmh = speed;
        alerts.save(a);
        log.info("ALERT [{}] {} - {}", severity, type, message);
        return a;
    }

    private Vehicle resolveVehicle(TrackingPayload p) {
        if (p.vehicleId() != null) {
            return vehicles.findById(p.vehicleId()).orElse(null);
        }
        if (p.vehicleCode() != null) {
            return vehicles.findByCode(p.vehicleCode()).orElse(null);
        }
        return null;
    }

    private boolean inside(Geofence f, double lat, double lng) {
        if (f.type != null && f.type.name().equals("POLYGON") && f.polygonJson != null && !f.polygonJson.isBlank()) {
            return pointInPolygon(f.polygonJson, lat, lng);
        }
        if (f.centerLat == null || f.centerLng == null) {
            return false;
        }
        double radiusKm = nz(f.radiusM) / 1000.0;
        return radiusKm > 0 && haversine(f.centerLat, f.centerLng, lat, lng) <= radiusKm;
    }

    /** polygonJson format: [[lat,lng],[lat,lng],...] */
    private boolean pointInPolygon(String json, double lat, double lng) {
        String cleaned = json.replace("[", "").replace("]", "").trim();
        if (cleaned.isEmpty()) {
            return false;
        }
        java.util.List<double[]> pts = new java.util.ArrayList<>();
        for (String pair : cleaned.split("[;\\n]")) {
            String[] nums = pair.split(",");
            if (nums.length >= 2) {
                try {
                    double plat = Double.parseDouble(nums[0].trim());
                    double plng = Double.parseDouble(nums[1].trim());
                    pts.add(new double[]{plat, plng});
                } catch (NumberFormatException ignored) {
                    // ignore malformed pairs
                }
            }
        }
        if (pts.size() < 3) {
            return false;
        }
        boolean result = false;
        for (int i = 0, j = pts.size() - 1; i < pts.size(); j = i++) {
            double yi = pts.get(i)[0], xi = pts.get(i)[1];
            double yj = pts.get(j)[0], xj = pts.get(j)[1];
            if (((yi > lat) != (yj > lat)) && (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi)) {
                result = !result;
            }
        }
        return result;
    }

    private void broadcast(Vehicle v, double lat, double lng, double speed, int heading, Instant ts,
                           LocationSource source) {
        String json = String.format(
                "{\"type\":\"position\",\"vehicleId\":%d,\"code\":\"%s\",\"lat\":%s,\"lng\":%s,"
                        + "\"speed\":%s,\"heading\":%d,\"status\":\"%s\",\"source\":\"%s\","
                        + "\"accuracy\":%s,\"ts\":\"%s\"}",
                v.id, v.code, lat, lng, speed, heading, v.status, source,
                v.lastAccuracy == null ? "null" : v.lastAccuracy, ts);
        ws.broadcast(json, v.companyId);
    }

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double nz(Double d) {
        return d == null ? 0.0 : d;
    }
}
