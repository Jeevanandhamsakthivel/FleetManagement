package com.fleet.web;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fleet.domain.Alert;
import com.fleet.repo.AlertRepository;
import com.fleet.security.CurrentUser;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRepository alerts;
    private final CurrentUser currentUser;

    public AlertController(AlertRepository alerts, CurrentUser currentUser) {
        this.alerts = alerts;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Alert> list() {
        return alerts.findAllByCompanyIdOrderByCreatedAtDesc(currentUser.companyId());
    }

    @GetMapping("/open")
    public List<Alert> open() {
        return alerts.findAllByCompanyIdAndAcknowledgedFalseOrderByCreatedAtDesc(currentUser.companyId());
    }

    @PostMapping("/{id}/acknowledge")
    public Alert acknowledge(@PathVariable Long id) {
        Alert a = alerts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        if (!a.companyId.equals(currentUser.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your alert");
        }
        a.acknowledged = true;
        a.acknowledgedAt = Instant.now();
        a.acknowledgedBy = currentUser.userId();
        return alerts.save(a);
    }
}
