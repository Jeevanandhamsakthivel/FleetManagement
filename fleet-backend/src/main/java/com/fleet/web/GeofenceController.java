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

import com.fleet.domain.Geofence;
import com.fleet.repo.GeofenceRepository;
import com.fleet.security.CurrentUser;
import com.fleet.service.AuditService;

@RestController
@RequestMapping("/api/geofences")
public class GeofenceController {

    private final GeofenceRepository geofences;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public GeofenceController(GeofenceRepository geofences, CurrentUser currentUser, AuditService audit) {
        this.geofences = geofences;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @GetMapping
    public List<Geofence> list() {
        return geofences.findAllByCompanyId(currentUser.companyId());
    }

    @PostMapping
    public Geofence create(@RequestBody Geofence body) {
        body.id = null;
        body.companyId = currentUser.companyId();
        body.createdAt = null;
        if (body.name == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        Geofence saved = geofences.save(body);
        audit.record(saved.companyId, currentUser.userId(), null, "CREATE", "Geofence", saved.id, "Created geofence " + saved.name);
        return saved;
    }

    @PutMapping("/{id}")
    public Geofence update(@PathVariable Long id, @RequestBody Geofence body) {
        Geofence g = require(id);
        body.id = g.id;
        body.companyId = g.companyId;
        body.createdAt = g.createdAt;
        return geofences.save(body);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Geofence g = require(id);
        geofences.delete(g);
    }

    private Geofence require(Long id) {
        Geofence g = geofences.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Geofence not found"));
        if (!g.companyId.equals(currentUser.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your geofence");
        }
        return g;
    }
}
