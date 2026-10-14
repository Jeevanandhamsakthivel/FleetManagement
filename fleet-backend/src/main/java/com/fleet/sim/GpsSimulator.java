package com.fleet.sim;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fleet.domain.LocationConfidence;
import com.fleet.domain.Vehicle;
import com.fleet.repo.VehicleRepository;
import com.fleet.service.TrackingService;
import com.fleet.web.dto.TrackingPayload;

/**
 * Simulates GPS/GNSS telematics devices so the live map works without physical hardware.
 * Disable with: app.gps.simulator.enabled=false
 */
@Component
@ConditionalOnProperty(name = "app.gps.simulator.enabled", havingValue = "true", matchIfMissing = true)
public class GpsSimulator {

    private static final double MIN_LAT = 12.850, MAX_LAT = 13.100;
    private static final double MIN_LNG = 77.450, MAX_LNG = 77.750;
    private static final double MAX_SPEED = 75.0;

    private final VehicleRepository vehicles;
    private final TrackingService tracking;
    private final Random random = new Random();
    private final Map<Long, double[]> state = new ConcurrentHashMap<>();

    @Value("${app.gps.simulator.interval-ms:3000}")
    private long intervalMs;

    public GpsSimulator(VehicleRepository vehicles, TrackingService tracking) {
        this.vehicles = vehicles;
        this.tracking = tracking;
    }

    @Scheduled(fixedRateString = "${app.gps.simulator.interval-ms:3000}")
    public void tick() {
        for (Vehicle v : vehicles.findAll()) {
            if (v.lastLat == null || v.lastLng == null) {
                continue;
            }
            if (v.status == com.fleet.domain.VehicleStatus.OFFLINE
                    || v.status == com.fleet.domain.VehicleStatus.MAINTENANCE) {
                continue;
            }
            double[] s = state.computeIfAbsent(v.id, id -> new double[]{random.nextDouble() * 360, 30 + random.nextDouble() * 30});

            // occasional turn
            if (random.nextDouble() < 0.3) {
                s[0] = (s[0] + (random.nextDouble() - 0.5) * 40 + 360) % 360;
            }
            // speed variation, sometimes stopping (idle)
            if (random.nextDouble() < 0.08) {
                s[1] = random.nextDouble() * 5; // idle
            } else {
                s[1] = Math.max(0, Math.min(MAX_SPEED, s[1] + (random.nextDouble() - 0.5) * 16));
            }
            double speed = s[1];
            double heading = s[0];

            double hours = intervalMs / 3_600_000.0;
            double distKm = speed * hours;
            double dLat = (distKm / 111.0) * Math.cos(Math.toRadians(heading));
            double dLng = (distKm / (111.0 * Math.cos(Math.toRadians(v.lastLat)))) * Math.sin(Math.toRadians(heading));

            double newLat = v.lastLat + dLat;
            double newLng = v.lastLng + dLng;
            if (newLat < MIN_LAT || newLat > MAX_LAT || newLng < MIN_LNG || newLng > MAX_LNG) {
                s[0] = (s[0] + 180) % 360;
                newLat = Math.max(MIN_LAT, Math.min(MAX_LAT, newLat));
                newLng = Math.max(MIN_LNG, Math.min(MAX_LNG, newLng));
            }

            TrackingPayload payload = new TrackingPayload(v.id, v.code, newLat, newLng, speed,
                    (int) Math.round(heading), speed > 0.5, Instant.now(), LocationConfidence.HIGH);
            try {
                tracking.ingest(payload);
            } catch (Exception ignored) {
                // keep the simulator alive even if a single ingest fails
            }
        }
    }
}
