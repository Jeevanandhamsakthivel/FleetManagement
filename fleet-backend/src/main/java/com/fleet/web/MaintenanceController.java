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

import com.fleet.domain.MaintenanceRecord;
import com.fleet.repo.MaintenanceRecordRepository;
import com.fleet.security.CurrentUser;
import com.fleet.service.AuditService;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceRecordRepository maintenance;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public MaintenanceController(MaintenanceRecordRepository maintenance, CurrentUser currentUser, AuditService audit) {
        this.maintenance = maintenance;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @GetMapping
    public List<MaintenanceRecord> list() {
        return maintenance.findAllByCompanyIdOrderByCreatedAtDesc(currentUser.companyId());
    }

    @PostMapping
    public MaintenanceRecord create(@RequestBody MaintenanceRecord body) {
        body.id = null;
        body.companyId = currentUser.companyId();
        body.createdAt = null;
        if (body.vehicleId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vehicleId is required");
        }
        MaintenanceRecord saved = maintenance.save(body);
        audit.record(saved.companyId, currentUser.userId(), null, "CREATE", "MaintenanceRecord", saved.id,
                "Created work order for vehicle " + saved.vehicleId);
        return saved;
    }

    @PutMapping("/{id}")
    public MaintenanceRecord update(@PathVariable Long id, @RequestBody MaintenanceRecord body) {
        MaintenanceRecord m = require(id);
        body.id = m.id;
        body.companyId = m.companyId;
        body.createdAt = m.createdAt;
        if (body.vehicleId == null) {
            body.vehicleId = m.vehicleId;
        }
        return maintenance.save(body);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        maintenance.delete(require(id));
    }

    private MaintenanceRecord require(Long id) {
        MaintenanceRecord m = maintenance.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));
        if (!m.companyId.equals(currentUser.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your record");
        }
        return m;
    }
}
