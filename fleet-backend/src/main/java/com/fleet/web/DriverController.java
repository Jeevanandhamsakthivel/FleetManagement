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

import com.fleet.domain.Driver;
import com.fleet.repo.DriverRepository;
import com.fleet.security.CurrentUser;
import com.fleet.service.AuditService;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverRepository drivers;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public DriverController(DriverRepository drivers, CurrentUser currentUser, AuditService audit) {
        this.drivers = drivers;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @GetMapping
    public List<Driver> list() {
        return drivers.findAllByCompanyId(currentUser.companyId());
    }

    @GetMapping("/{id}")
    public Driver get(@PathVariable Long id) {
        return require(id);
    }

    @PostMapping
    public Driver create(@RequestBody Driver body) {
        body.id = null;
        body.companyId = currentUser.companyId();
        body.createdAt = null;
        if (body.fullName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fullName is required");
        }
        Driver saved = drivers.save(body);
        audit.record(saved.companyId, currentUser.userId(), null, "CREATE", "Driver", saved.id, "Created driver " + saved.fullName);
        return saved;
    }

    @PutMapping("/{id}")
    public Driver update(@PathVariable Long id, @RequestBody Driver body) {
        Driver d = require(id);
        body.id = d.id;
        body.companyId = d.companyId;
        body.createdAt = d.createdAt;
        Driver saved = drivers.save(body);
        audit.record(saved.companyId, currentUser.userId(), null, "UPDATE", "Driver", saved.id, "Updated driver " + saved.fullName);
        return saved;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Driver d = require(id);
        drivers.delete(d);
        audit.record(d.companyId, currentUser.userId(), null, "DELETE", "Driver", id, "Deleted driver " + d.fullName);
    }

    private Driver require(Long id) {
        Driver d = drivers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        if (!d.companyId.equals(currentUser.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your driver");
        }
        return d;
    }
}
