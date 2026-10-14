package com.fleet.web;

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

import com.fleet.domain.Vehicle;
import com.fleet.domain.VehicleDriverAssignment;
import com.fleet.domain.VehicleStatus;
import com.fleet.repo.VehicleDriverAssignmentRepository;
import com.fleet.repo.VehicleRepository;
import com.fleet.security.CurrentUser;
import com.fleet.service.AuditService;
import com.fleet.web.dto.AssignRequest;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleRepository vehicles;
    private final VehicleDriverAssignmentRepository assignments;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public VehicleController(VehicleRepository vehicles, VehicleDriverAssignmentRepository assignments,
                             CurrentUser currentUser, AuditService audit) {
        this.vehicles = vehicles;
        this.assignments = assignments;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @GetMapping
    public List<Vehicle> list() {
        return vehicles.findAllByCompanyId(currentUser.companyId());
    }

    @GetMapping("/live")
    public List<Vehicle> live() {
        return vehicles.findAllByCompanyId(currentUser.companyId()).stream()
                .filter(v -> v.lastLat != null && v.lastLng != null && v.status != VehicleStatus.OFFLINE)
                .toList();
    }

    @GetMapping("/{id}")
    public Vehicle get(@PathVariable Long id) {
        return require(id);
    }

    @PostMapping
    public Vehicle create(@RequestBody Vehicle body) {
        body.id = null;
        body.companyId = currentUser.companyId();
        body.createdAt = null;
        if (body.code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code is required");
        }
        if (vehicles.findByCompanyIdAndCode(body.companyId, body.code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle code already exists");
        }
        Vehicle saved = vehicles.save(body);
        audit.record(saved.companyId, currentUser.userId(), currentUser.get() != null ? currentUser.get().username() : null,
                "CREATE", "Vehicle", saved.id, "Created vehicle " + saved.code);
        return saved;
    }

    @PutMapping("/{id}")
    public Vehicle update(@PathVariable Long id, @RequestBody Vehicle body) {
        Vehicle v = require(id);
        body.id = v.id;
        body.companyId = v.companyId;
        body.createdAt = v.createdAt;
        if (body.lastLat == null) {
            body.lastLat = v.lastLat;
        }
        if (body.lastLng == null) {
            body.lastLng = v.lastLng;
        }
        Vehicle saved = vehicles.save(body);
        audit.record(saved.companyId, currentUser.userId(), null, "UPDATE", "Vehicle", saved.id, "Updated vehicle " + saved.code);
        return saved;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Vehicle v = require(id);
        vehicles.delete(v);
        audit.record(v.companyId, currentUser.userId(), null, "DELETE", "Vehicle", id, "Deleted vehicle " + v.code);
    }

    @PostMapping("/{id}/assign")
    public VehicleDriverAssignment assign(@PathVariable Long id, @RequestBody AssignRequest req) {
        Vehicle v = require(id);
        assignments.findFirstByVehicleIdAndActiveTrue(id).ifPresent(a -> {
            a.active = false;
            a.releasedAt = java.time.Instant.now();
            assignments.save(a);
        });
        VehicleDriverAssignment a = new VehicleDriverAssignment();
        a.companyId = v.companyId;
        a.vehicleId = id;
        a.driverId = req.driverId();
        a.active = true;
        VehicleDriverAssignment saved = assignments.save(a);
        audit.record(v.companyId, currentUser.userId(), null, "ASSIGN", "Vehicle", id,
                "Assigned driver " + req.driverId() + " to vehicle " + v.code);
        return saved;
    }

    private Vehicle require(Long id) {
        Vehicle v = vehicles.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        if (!v.companyId.equals(currentUser.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your vehicle");
        }
        return v;
    }
}
