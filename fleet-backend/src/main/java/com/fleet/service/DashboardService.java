package com.fleet.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fleet.domain.DeliveryStatus;
import com.fleet.domain.DriverStatus;
import com.fleet.domain.FuelRecord;
import com.fleet.domain.MaintenanceRecord;
import com.fleet.domain.TripStatus;
import com.fleet.domain.Vehicle;
import com.fleet.domain.VehicleStatus;
import com.fleet.repo.AlertRepository;
import com.fleet.repo.DeliveryRepository;
import com.fleet.repo.DriverRepository;
import com.fleet.repo.FuelRecordRepository;
import com.fleet.repo.MaintenanceRecordRepository;
import com.fleet.repo.TripRepository;
import com.fleet.repo.VehicleRepository;

@Service
public class DashboardService {

    private final VehicleRepository vehicles;
    private final DriverRepository drivers;
    private final TripRepository trips;
    private final AlertRepository alerts;
    private final DeliveryRepository deliveries;
    private final FuelRecordRepository fuel;
    private final MaintenanceRecordRepository maintenance;

    public DashboardService(VehicleRepository vehicles, DriverRepository drivers, TripRepository trips,
                            AlertRepository alerts, DeliveryRepository deliveries, FuelRecordRepository fuel,
                            MaintenanceRecordRepository maintenance) {
        this.vehicles = vehicles;
        this.drivers = drivers;
        this.trips = trips;
        this.alerts = alerts;
        this.deliveries = deliveries;
        this.fuel = fuel;
        this.maintenance = maintenance;
    }

    public Map<String, Object> kpis(Long companyId) {
        long total = vehicles.countByCompanyId(companyId);
        long moving = vehicles.countByCompanyIdAndStatus(companyId, VehicleStatus.MOVING);

        double distance = 0;
        for (Vehicle v : vehicles.findAllByCompanyId(companyId)) {
            distance += v.odometer == null ? 0 : v.odometer;
        }
        double fuelCost = 0;
        for (FuelRecord f : fuel.findAllByCompanyIdOrderByFilledAtDesc(companyId)) {
            fuelCost += f.cost == null ? 0 : f.cost;
        }
        double maintenanceCost = 0;
        for (MaintenanceRecord m : maintenance.findAllByCompanyIdOrderByCreatedAtDesc(companyId)) {
            maintenanceCost += m.cost == null ? 0 : m.cost;
        }

        Map<String, Object> k = new LinkedHashMap<>();
        k.put("totalVehicles", total);
        k.put("moving", moving);
        k.put("idle", vehicles.countByCompanyIdAndStatus(companyId, VehicleStatus.IDLE));
        k.put("stopped", vehicles.countByCompanyIdAndStatus(companyId, VehicleStatus.STOPPED));
        k.put("offline", vehicles.countByCompanyIdAndStatus(companyId, VehicleStatus.OFFLINE));
        k.put("maintenance", vehicles.countByCompanyIdAndStatus(companyId, VehicleStatus.MAINTENANCE));
        k.put("totalDrivers", drivers.countByCompanyId(companyId));
        k.put("driversAvailable", drivers.countByCompanyIdAndStatus(companyId, DriverStatus.AVAILABLE));
        k.put("driversOnTrip", drivers.countByCompanyIdAndStatus(companyId, DriverStatus.ON_TRIP));
        k.put("activeTrips", trips.countByCompanyIdAndStatus(companyId, TripStatus.IN_PROGRESS));
        k.put("openAlerts", alerts.countByCompanyIdAndAcknowledgedFalse(companyId));
        k.put("deliveriesInTransit", deliveries.countByCompanyIdAndStatus(companyId, DeliveryStatus.IN_TRANSIT));
        k.put("deliveriesDelivered", deliveries.countByCompanyIdAndStatus(companyId, DeliveryStatus.DELIVERED));
        k.put("totalDistanceKm", round(distance));
        k.put("fuelCost", round(fuelCost));
        k.put("maintenanceCost", round(maintenanceCost));
        k.put("costPerKm", distance > 0 ? round((fuelCost + maintenanceCost) / distance) : 0.0);
        k.put("utilizationPct", total > 0 ? round(moving * 100.0 / total) : 0.0);
        k.put("vehicleStatusBreakdown", List.of(moving,
                vehicles.countByCompanyIdAndStatus(companyId, VehicleStatus.IDLE),
                vehicles.countByCompanyIdAndStatus(companyId, VehicleStatus.STOPPED),
                vehicles.countByCompanyIdAndStatus(companyId, VehicleStatus.OFFLINE),
                vehicles.countByCompanyIdAndStatus(companyId, VehicleStatus.MAINTENANCE)));
        return k;
    }

    private static double round(double d) {
        return Math.round(d * 100.0) / 100.0;
    }
}
