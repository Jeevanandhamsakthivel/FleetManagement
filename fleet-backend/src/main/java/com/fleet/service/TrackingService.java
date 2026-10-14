package com.fleet.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fleet.domain.Alert;
import com.fleet.domain.AlertSeverity;
import com.fleet.domain.AlertType;
import com.fleet.domain.Geofence;
import com.fleet.domain.LocationConfidence;
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

        Double prevLat = v.lastLat;
        Double prevLng = v.lastLng;
        Double prevSpeed = v.lastSpeed;
        boolean hadPrev = prevLat != null && prevLng != null;

        Trip active = trips.findFirstByVehicleIdAndStatus(v.id, TripStatus.IN_PROGRESS).orElse(null);

        TripLocation loc = new TripLocation();
        loc.vehicleId = v.id;
        loc.tripId = active != null ? active.id : null;
        loc.latitude = lat;
        loc.longitude = lng;
        loc.speedKmh = speed;
        loc.heading = heading;
        loc.ignition = ignition;
        loc.recordedAt = ts;
        loc.confidence = p.confidence() != null ? p.confidence() : LocationConfidence.HIGH;
        locations.save(loc);

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
        v.confidence = loc.confidence;
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

        broadcast(v, lat, lng, speed, heading, ts);
        return loc;
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

    private void broadcast(Vehicle v, double lat, double lng, double speed, int heading, Instant ts) {
        String json = String.format(
                "{\"type\":\"position\",\"vehicleId\":%d,\"code\":\"%s\",\"lat\":%s,\"lng\":%s,"
                        + "\"speed\":%s,\"heading\":%d,\"status\":\"%s\",\"ts\":\"%s\"}",
                v.id, v.code, lat, lng, speed, heading, v.status, ts);
        ws.broadcast(json);
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
