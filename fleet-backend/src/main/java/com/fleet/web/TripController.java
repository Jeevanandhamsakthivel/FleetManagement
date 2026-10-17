package com.fleet.web;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fleet.domain.Driver;
import com.fleet.domain.DriverStatus;
import com.fleet.domain.RouteState;
import com.fleet.domain.Trip;
import com.fleet.domain.TripLocation;
import com.fleet.domain.TripStatus;
import com.fleet.domain.Vehicle;
import com.fleet.domain.VehicleStatus;
import com.fleet.repo.DriverRepository;
import com.fleet.repo.TripLocationRepository;
import com.fleet.repo.TripRepository;
import com.fleet.repo.VehicleRepository;
import com.fleet.security.CurrentUser;
import com.fleet.service.AuditService;
import com.fleet.service.RouteGeometry;
import com.fleet.service.RoutingService;
import com.fleet.web.dto.CorridorRequest;
import com.fleet.web.dto.RouteResponse;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripRepository trips;
    private final TripLocationRepository locations;
    private final VehicleRepository vehicles;
    private final DriverRepository drivers;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final RoutingService routing;

    public TripController(TripRepository trips, TripLocationRepository locations, VehicleRepository vehicles,
                          DriverRepository drivers, CurrentUser currentUser, AuditService audit,
                          RoutingService routing) {
        this.trips = trips;
        this.locations = locations;
        this.vehicles = vehicles;
        this.drivers = drivers;
        this.currentUser = currentUser;
        this.audit = audit;
        this.routing = routing;
    }

    @GetMapping
    public List<Trip> list() {
        return trips.findAllByCompanyIdOrderByCreatedAtDesc(currentUser.companyId());
    }

    @GetMapping("/{id}")
    public Trip get(@PathVariable Long id) {
        return require(id);
    }

    @GetMapping("/{id}/locations")
    public List<TripLocation> tripLocations(@PathVariable Long id) {
        require(id);
        return locations.findAllByTripIdOrderByRecordedAtAsc(id);
    }

    @PostMapping
    public Trip create(@RequestBody Trip body) {
        body.id = null;
        body.companyId = currentUser.companyId();
        body.createdAt = null;
        if (body.status == null) {
            body.status = TripStatus.PLANNED;
        }
        if (body.deviationThresholdM == null || body.deviationThresholdM < 50) {
            body.deviationThresholdM = 500;
        }
        if (body.deviationConfirmations == null || body.deviationConfirmations < 1) {
            body.deviationConfirmations = 3;
        }
        Trip saved = trips.save(body);
        // Best-effort route planning; a routing outage must not block trip creation.
        planRoute(saved);
        audit.record(saved.companyId, currentUser.userId(), null, "CREATE", "Trip", saved.id,
                "Created trip " + saved.origin + " -> " + saved.destination);
        return trips.save(saved);
    }

    /**
     * Resolves the trip's origin/destination to real coordinates and fetches an actual road
     * route from the routing provider. If either step fails the geometry stays null so the UI
     * can show an honest "route unavailable" state instead of a fabricated straight line.
     */
    private void planRoute(Trip t) {
        try {
            double[] origin = null;
            double[] dest = null;
            if (t.originLat != null && t.originLng != null) {
                origin = new double[]{t.originLat, t.originLng};
            } else if (t.origin != null && !t.origin.isBlank()) {
                origin = routing.geocode(t.origin);
            }
            if (t.destLat != null && t.destLng != null) {
                dest = new double[]{t.destLat, t.destLng};
            } else if (t.destination != null && !t.destination.isBlank()) {
                dest = routing.geocode(t.destination);
            }
            if (origin == null || dest == null) {
                return;
            }
            t.originLat = origin[0];
            t.originLng = origin[1];
            t.destLat = dest[0];
            t.destLng = dest[1];
            RoutingService.Route route = routing.route(origin[0], origin[1], dest[0], dest[1]);
            if (route == null) {
                return;
            }
            t.routeGeometry = RouteGeometry.toJson(route.geometry());
            t.routeProvider = route.provider();
            t.routeDistanceKm = route.distanceKm();
        } catch (Exception ignored) {
            // Routes are optional; never fail the request because a provider was unreachable.
        }
    }

    @PutMapping("/{id}")
    public Trip update(@PathVariable Long id, @RequestBody Trip body) {
        Trip t = require(id);
        body.id = t.id;
        body.companyId = t.companyId;
        body.createdAt = t.createdAt;
        if (body.distanceKm == null) {
            body.distanceKm = t.distanceKm;
        }
        // Preserve server-computed route/corridor/monitoring fields unless explicitly re-planned.
        if (body.routeGeometry == null) {
            body.routeGeometry = t.routeGeometry;
            body.routeProvider = t.routeProvider;
            body.routeDistanceKm = t.routeDistanceKm;
            body.originLat = t.originLat;
            body.originLng = t.originLng;
            body.destLat = t.destLat;
            body.destLng = t.destLng;
        }
        if (body.deviationThresholdM == null) {
            body.deviationThresholdM = t.deviationThresholdM;
        }
        if (body.deviationConfirmations == null) {
            body.deviationConfirmations = t.deviationConfirmations;
        }
        body.routeState = t.routeState;
        body.lastDeviationM = t.lastDeviationM;
        body.lastDeviationAt = t.lastDeviationAt;
        body.deviationStreak = t.deviationStreak;
        body.lastGpsAt = t.lastGpsAt;
        return trips.save(body);
    }

    @PostMapping("/{id}/start")
    public Trip start(@PathVariable Long id) {
        Trip t = require(id);
        if (t.routeGeometry == null || t.routeGeometry.isBlank()) {
            planRoute(t);
        }
        t.status = TripStatus.IN_PROGRESS;
        t.actualStart = Instant.now();
        t.routeState = RouteState.ON_ROUTE;
        t.deviationStreak = 0;
        t.lastGpsAt = null;
        if (t.vehicleId != null) {
            vehicles.findById(t.vehicleId).ifPresent(v -> {
                t.startOdometer = v.odometer;
                v.status = VehicleStatus.MOVING;
                v.lastIgnition = true;
                vehicles.save(v);
            });
        }
        if (t.driverId != null) {
            drivers.findById(t.driverId).ifPresent(d -> {
                d.status = DriverStatus.ON_TRIP;
                drivers.save(d);
            });
        }
        audit.record(t.companyId, currentUser.userId(), null, "START", "Trip", id, "Trip started");
        return trips.save(t);
    }

    @PostMapping("/{id}/complete")
    public Trip complete(@PathVariable Long id) {
        Trip t = require(id);
        t.status = TripStatus.COMPLETED;
        t.routeState = RouteState.COMPLETED;
        t.actualEnd = Instant.now();
        if (t.vehicleId != null) {
            vehicles.findById(t.vehicleId).ifPresent(v -> {
                t.endOdometer = v.odometer;
                if (t.distanceKm == null || t.distanceKm == 0) {
                    t.distanceKm = (t.startOdometer != null && t.endOdometer != null)
                            ? Math.max(0, t.endOdometer - t.startOdometer) : 0.0;
                }
                v.status = VehicleStatus.IDLE;
                v.lastIgnition = false;
                v.lastSpeed = 0.0;
                vehicles.save(v);
            });
        }
        if (t.driverId != null) {
            drivers.findById(t.driverId).ifPresent(d -> {
                d.status = DriverStatus.AVAILABLE;
                drivers.save(d);
            });
        }
        audit.record(t.companyId, currentUser.userId(), null, "COMPLETE", "Trip", id, "Trip completed");
        return trips.save(t);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Trip t = require(id);
        trips.delete(t);
        audit.record(t.companyId, currentUser.userId(), null, "DELETE", "Trip", id, "Deleted trip");
    }

    /** Planned road route + corridor settings + current route state for a trip. */
    @GetMapping("/{id}/route")
    public RouteResponse route(@PathVariable Long id) {
        return toRouteResponse(require(id));
    }

    /** Re-runs geocoding/route planning (useful after a provider outage or a name edit). */
    @PostMapping("/{id}/route")
    public RouteResponse replanRoute(@PathVariable Long id) {
        Trip t = require(id);
        planRoute(t);
        if (t.routeGeometry == null || t.routeGeometry.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not resolve a road route for '" + t.origin + "' -> '" + t.destination + "'");
        }
        trips.save(t);
        audit.record(t.companyId, currentUser.userId(), null, "PLAN_ROUTE", "Trip", id,
                "Planned route " + t.origin + " -> " + t.destination);
        return toRouteResponse(t);
    }

    /** Updates the permitted route corridor (metres) and required confirmations for a trip. */
    @PutMapping("/{id}/corridor")
    public RouteResponse updateCorridor(@PathVariable Long id, @RequestBody CorridorRequest body) {
        Trip t = require(id);
        if (body.deviationThresholdM() != null) {
            if (body.deviationThresholdM() < 50 || body.deviationThresholdM() > 20000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "deviationThresholdM must be between 50 and 20000");
            }
            t.deviationThresholdM = body.deviationThresholdM();
        }
        if (body.deviationConfirmations() != null) {
            if (body.deviationConfirmations() < 1 || body.deviationConfirmations() > 20) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "deviationConfirmations must be between 1 and 20");
            }
            t.deviationConfirmations = body.deviationConfirmations();
        }
        trips.save(t);
        audit.record(t.companyId, currentUser.userId(), null, "UPDATE_CORRIDOR", "Trip", id,
                "Corridor " + t.deviationThresholdM + "m / " + t.deviationConfirmations + " confirmations");
        return toRouteResponse(t);
    }

    /** All in-progress trips that have a planned route, so the dashboard can draw every corridor. */
    @GetMapping("/active-routes")
    public List<RouteResponse> activeRoutes() {
        List<RouteResponse> result = new ArrayList<>();
        for (Trip t : trips.findAllByCompanyIdAndStatus(currentUser.companyId(), TripStatus.IN_PROGRESS)) {
            if (t.routeGeometry != null && !t.routeGeometry.isBlank()) {
                result.add(toRouteResponse(t));
            }
        }
        return result;
    }

    private RouteResponse toRouteResponse(Trip t) {
        List<double[]> geometry = RouteGeometry.fromJson(t.routeGeometry);
        return new RouteResponse(t.id, t.vehicleId, t.driverId, t.origin, t.destination,
                t.originLat, t.originLng, t.destLat, t.destLng, t.routeProvider, t.routeDistanceKm,
                t.deviationThresholdM, t.deviationConfirmations,
                t.routeState != null ? t.routeState.name() : null, t.lastDeviationM, geometry);
    }

    private Trip require(Long id) {
        Trip t = trips.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
        if (!t.companyId.equals(currentUser.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your trip");
        }
        return t;
    }
}
