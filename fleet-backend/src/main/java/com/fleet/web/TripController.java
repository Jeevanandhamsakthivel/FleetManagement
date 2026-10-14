package com.fleet.web;

import java.time.Instant;
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

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripRepository trips;
    private final TripLocationRepository locations;
    private final VehicleRepository vehicles;
    private final DriverRepository drivers;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public TripController(TripRepository trips, TripLocationRepository locations, VehicleRepository vehicles,
                          DriverRepository drivers, CurrentUser currentUser, AuditService audit) {
        this.trips = trips;
        this.locations = locations;
        this.vehicles = vehicles;
        this.drivers = drivers;
        this.currentUser = currentUser;
        this.audit = audit;
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
        Trip saved = trips.save(body);
        audit.record(saved.companyId, currentUser.userId(), null, "CREATE", "Trip", saved.id,
                "Created trip " + saved.origin + " -> " + saved.destination);
        return saved;
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
        return trips.save(body);
    }

    @PostMapping("/{id}/start")
    public Trip start(@PathVariable Long id) {
        Trip t = require(id);
        t.status = TripStatus.IN_PROGRESS;
        t.actualStart = Instant.now();
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

    private Trip require(Long id) {
        Trip t = trips.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
        if (!t.companyId.equals(currentUser.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your trip");
        }
        return t;
    }
}
