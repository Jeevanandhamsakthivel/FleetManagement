package com.fleet.web;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fleet.domain.TripLocation;
import com.fleet.domain.Vehicle;
import com.fleet.domain.VehicleStatus;
import com.fleet.repo.TripLocationRepository;
import com.fleet.repo.VehicleRepository;
import com.fleet.security.CurrentUser;
import com.fleet.service.TrackingService;
import com.fleet.web.dto.TrackingPayload;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final TrackingService tracking;
    private final VehicleRepository vehicles;
    private final TripLocationRepository locations;
    private final CurrentUser currentUser;

    public TrackingController(TrackingService tracking, VehicleRepository vehicles, TripLocationRepository locations,
                              CurrentUser currentUser) {
        this.tracking = tracking;
        this.vehicles = vehicles;
        this.locations = locations;
        this.currentUser = currentUser;
    }

    /** GPS/GNSS device ingestion endpoint (telematics -> backend). */
    @PostMapping
    public TripLocation ingest(@RequestBody TrackingPayload payload) {
        if (payload.latitude() == null || payload.longitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude and longitude are required");
        }
        if (payload.latitude() < -90 || payload.latitude() > 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude must be between -90 and 90");
        }
        if (payload.longitude() < -180 || payload.longitude() > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "longitude must be between -180 and 180");
        }
        if (payload.accuracy() != null && payload.accuracy() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accuracy must not be negative");
        }
        // Reject updates for vehicles outside the caller's company.
        Vehicle v = resolveOwnedVehicle(payload);
        if (v == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown vehicle");
        }
        return tracking.ingest(payload);
    }

    private Vehicle resolveOwnedVehicle(TrackingPayload payload) {
        Vehicle v = payload.vehicleId() != null
                ? vehicles.findById(payload.vehicleId()).orElse(null)
                : payload.vehicleCode() != null
                        ? vehicles.findByCode(payload.vehicleCode()).orElse(null)
                        : null;
        if (v == null) {
            return null;
        }
        if (!v.companyId.equals(currentUser.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your vehicle");
        }
        return v;
    }

    @GetMapping("/live")
    public List<Vehicle> live() {
        return vehicles.findAllByCompanyId(currentUser.companyId()).stream()
                .filter(v -> v.lastLat != null && v.lastLng != null && v.status != VehicleStatus.OFFLINE)
                .toList();
    }

    @GetMapping("/vehicle/{vehicleId}/history")
    public List<TripLocation> history(@PathVariable Long vehicleId) {
        Vehicle v = vehicles.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        if (!v.companyId.equals(currentUser.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your vehicle");
        }
        return locations.findAllByVehicleIdAndRecordedAtAfterOrderByRecordedAtAsc(vehicleId,
                Instant.now().minus(6, ChronoUnit.HOURS));
    }
}
