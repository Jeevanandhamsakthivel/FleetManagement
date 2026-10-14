package com.fleet.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

import com.fleet.domain.Delivery;
import com.fleet.domain.DeliveryStatus;
import com.fleet.domain.ProofOfDelivery;
import com.fleet.repo.DeliveryRepository;
import com.fleet.repo.ProofOfDeliveryRepository;
import com.fleet.security.CurrentUser;
import com.fleet.service.AuditService;
import com.fleet.web.dto.AssignRequest;
import com.fleet.web.dto.PodRequest;
import com.fleet.web.dto.StatusUpdateRequest;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryRepository deliveries;
    private final ProofOfDeliveryRepository pods;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public DeliveryController(DeliveryRepository deliveries, ProofOfDeliveryRepository pods, CurrentUser currentUser,
                              AuditService audit) {
        this.deliveries = deliveries;
        this.pods = pods;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @GetMapping
    public List<Delivery> list() {
        return deliveries.findAllByCompanyIdOrderByCreatedAtDesc(currentUser.companyId());
    }

    @GetMapping("/{id}")
    public Delivery get(@PathVariable Long id) {
        return require(id);
    }

    @PostMapping
    public Delivery create(@RequestBody Delivery body) {
        body.id = null;
        body.companyId = currentUser.companyId();
        body.createdAt = null;
        if (body.trackingToken == null) {
            body.trackingToken = UUID.randomUUID().toString().replace("-", "");
        }
        if (body.reference == null) {
            body.reference = "ORD-" + System.currentTimeMillis() % 100000;
        }
        Delivery saved = deliveries.save(body);
        audit.record(saved.companyId, currentUser.userId(), null, "CREATE", "Delivery", saved.id,
                "Created delivery " + saved.reference);
        return saved;
    }

    @PutMapping("/{id}")
    public Delivery update(@PathVariable Long id, @RequestBody Delivery body) {
        Delivery d = require(id);
        body.id = d.id;
        body.companyId = d.companyId;
        body.createdAt = d.createdAt;
        if (body.trackingToken == null) {
            body.trackingToken = d.trackingToken;
        }
        return deliveries.save(body);
    }

    @PostMapping("/{id}/assign")
    public Delivery assign(@PathVariable Long id, @RequestBody AssignRequest req) {
        Delivery d = require(id);
        d.vehicleId = req.vehicleId();
        d.driverId = req.driverId();
        if (d.status == DeliveryStatus.PENDING) {
            d.status = DeliveryStatus.ASSIGNED;
        }
        audit.record(d.companyId, currentUser.userId(), null, "ASSIGN", "Delivery", id, "Delivery assigned");
        return deliveries.save(d);
    }

    @PostMapping("/{id}/status")
    public Delivery setStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest req) {
        Delivery d = require(id);
        d.status = DeliveryStatus.valueOf(req.status());
        if (d.status == DeliveryStatus.DELIVERED) {
            d.deliveredAt = Instant.now();
        }
        audit.record(d.companyId, currentUser.userId(), null, "STATUS", "Delivery", id, "Status -> " + d.status);
        return deliveries.save(d);
    }

    @PostMapping("/{id}/proof-of-delivery")
    public ProofOfDelivery addPod(@PathVariable Long id, @RequestBody PodRequest req) {
        Delivery d = require(id);
        ProofOfDelivery pod = new ProofOfDelivery();
        pod.deliveryId = id;
        pod.type = req.type();
        pod.photoUrl = req.photoUrl();
        pod.signature = req.signature();
        pod.otp = req.otp();
        pod.recipientName = req.recipientName();
        pod.capturedAt = Instant.now();
        ProofOfDelivery saved = pods.save(pod);
        d.status = DeliveryStatus.DELIVERED;
        d.deliveredAt = Instant.now();
        deliveries.save(d);
        audit.record(d.companyId, currentUser.userId(), null, "POD", "Delivery", id, "Proof of delivery captured");
        return saved;
    }

    @GetMapping("/{id}/proof-of-delivery")
    public List<ProofOfDelivery> listPod(@PathVariable Long id) {
        require(id);
        return pods.findAllByDeliveryId(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        deliveries.delete(require(id));
    }

    private Delivery require(Long id) {
        Delivery d = deliveries.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
        if (!d.companyId.equals(currentUser.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your delivery");
        }
        return d;
    }
}
