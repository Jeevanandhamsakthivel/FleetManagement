package com.fleet.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fleet.domain.Delivery;
import com.fleet.domain.Vehicle;
import com.fleet.repo.DeliveryRepository;
import com.fleet.repo.VehicleRepository;

/** Public, token-based customer delivery tracking (no login required). */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final DeliveryRepository deliveries;
    private final VehicleRepository vehicles;

    public PublicController(DeliveryRepository deliveries, VehicleRepository vehicles) {
        this.deliveries = deliveries;
        this.vehicles = vehicles;
    }

    @GetMapping("/track/{token}")
    public Map<String, Object> track(@PathVariable String token) {
        Delivery d = deliveries.findByTrackingToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracking link not found"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reference", d.reference);
        body.put("status", d.status);
        body.put("pickupAddress", d.pickupAddress);
        body.put("deliveryAddress", d.deliveryAddress);
        body.put("scheduledAt", d.scheduledAt);
        body.put("eta", d.eta);
        body.put("deliveredAt", d.deliveredAt);

        if (d.vehicleId != null) {
            Vehicle v = vehicles.findById(d.vehicleId).orElse(null);
            if (v != null) {
                Map<String, Object> pos = new LinkedHashMap<>();
                pos.put("code", v.code);
                pos.put("lat", v.lastLat);
                pos.put("lng", v.lastLng);
                pos.put("speed", v.lastSpeed);
                pos.put("status", v.status);
                pos.put("lastUpdate", v.lastUpdate);
                body.put("vehicle", pos);
            }
        }
        return body;
    }
}
