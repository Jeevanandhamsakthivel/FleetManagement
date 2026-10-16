package com.fleet.config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fleet.domain.AppUser;
import com.fleet.domain.Company;
import com.fleet.domain.Customer;
import com.fleet.domain.Delivery;
import com.fleet.domain.DeliveryStatus;
import com.fleet.domain.Driver;
import com.fleet.domain.DriverStatus;
import com.fleet.domain.FuelRecord;
import com.fleet.domain.FuelType;
import com.fleet.domain.Geofence;
import com.fleet.domain.GeofenceType;
import com.fleet.domain.LocationConfidence;
import com.fleet.domain.LocationSource;
import com.fleet.domain.MaintenanceRecord;
import com.fleet.domain.MaintenanceType;
import com.fleet.domain.Role;
import com.fleet.domain.Trip;
import com.fleet.domain.TripStatus;
import com.fleet.domain.Vehicle;
import com.fleet.domain.VehicleDocument;
import com.fleet.domain.VehicleDriverAssignment;
import com.fleet.domain.VehicleStatus;
import com.fleet.repo.AppUserRepository;
import com.fleet.repo.CompanyRepository;
import com.fleet.repo.CustomerRepository;
import com.fleet.repo.DeliveryRepository;
import com.fleet.repo.DriverRepository;
import com.fleet.repo.FuelRecordRepository;
import com.fleet.repo.GeofenceRepository;
import com.fleet.repo.MaintenanceRecordRepository;
import com.fleet.repo.TripRepository;
import com.fleet.repo.VehicleDocumentRepository;
import com.fleet.repo.VehicleDriverAssignmentRepository;
import com.fleet.repo.VehicleRepository;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CompanyRepository companies;
    private final AppUserRepository users;
    private final VehicleRepository vehicles;
    private final DriverRepository drivers;
    private final VehicleDriverAssignmentRepository assignments;
    private final TripRepository trips;
    private final GeofenceRepository geofences;
    private final CustomerRepository customers;
    private final DeliveryRepository deliveries;
    private final MaintenanceRecordRepository maintenance;
    private final FuelRecordRepository fuel;
    private final VehicleDocumentRepository documents;
    private final PasswordEncoder encoder;

    public DataSeeder(CompanyRepository companies, AppUserRepository users, VehicleRepository vehicles,
                      DriverRepository drivers, VehicleDriverAssignmentRepository assignments, TripRepository trips,
                      GeofenceRepository geofences, CustomerRepository customers, DeliveryRepository deliveries,
                      MaintenanceRecordRepository maintenance, FuelRecordRepository fuel,
                      VehicleDocumentRepository documents, PasswordEncoder encoder) {
        this.companies = companies;
        this.users = users;
        this.vehicles = vehicles;
        this.drivers = drivers;
        this.assignments = assignments;
        this.trips = trips;
        this.geofences = geofences;
        this.customers = customers;
        this.deliveries = deliveries;
        this.maintenance = maintenance;
        this.fuel = fuel;
        this.documents = documents;
        this.encoder = encoder;
    }

    /**
     * When the GPS simulator is disabled (e.g. the real-tracking MySQL profile) seeded
     * vehicles carry no synthetic coordinates, so the map shows an honest empty state
     * until a genuine mobile/device GPS position arrives.
     */
    @Value("${app.gps.simulator.enabled:true}")
    private boolean simulatorEnabled;

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }

        Company company = new Company();
        company.name = "Acme Logistics";
        company.email = "ops@acme-logistics.example";
        company.phone = "+91 80 1234 5678";
        company.address = "Bengaluru, India";
        company.timezone = "Asia/Kolkata";
        company = companies.save(company);
        Long cid = company.id;

        user(cid, "admin", "admin123", "Ravi Kumar", "admin@acme.example", Role.COMPANY_ADMIN);
        user(cid, "manager", "manager123", "Priya Sharma", "manager@acme.example", Role.FLEET_MANAGER);
        user(cid, "dispatch", "dispatch123", "Arjun Nair", "dispatch@acme.example", Role.DISPATCHER);
        user(cid, "driver", "driver123", "Sunil Yadav", "driver@acme.example", Role.DRIVER);

        Driver d1 = driver(cid, "Sunil Yadav", "+91 90000 00001", "KA01-2019-0001");
        Driver d2 = driver(cid, "Meena Rao", "+91 90000 00002", "KA01-2018-0002");
        Driver d3 = driver(cid, "Imran Khan", "+91 90000 00003", "KA01-2020-0003");
        Driver d4 = driver(cid, "Kavya Menon", "+91 90000 00004", "KA01-2021-0004");
        d1.status = DriverStatus.ON_TRIP;
        d2.status = DriverStatus.ON_TRIP;
        drivers.save(d1);
        drivers.save(d2);

        Vehicle v1 = vehicle(cid, "VH-101", "KA01 AB 1234", "Truck", "Tata", "Ace", 2021, FuelType.DIESEL, 12.9716, 77.5946, VehicleStatus.MOVING);
        Vehicle v2 = vehicle(cid, "VH-102", "KA01 CD 5678", "Van", "Mahindra", "Supro", 2022, FuelType.DIESEL, 12.9352, 77.6245, VehicleStatus.MOVING);
        Vehicle v3 = vehicle(cid, "VH-103", "KA01 EF 9012", "Car", "Tata", "Tiago EV", 2023, FuelType.ELECTRIC, 12.9980, 77.5700, VehicleStatus.IDLE);
        Vehicle v4 = vehicle(cid, "VH-104", "KA01 GH 3456", "Truck", "Ashok Leyland", "Dost", 2020, FuelType.CNG, 13.0120, 77.6410, VehicleStatus.IDLE);
        Vehicle v5 = vehicle(cid, "VH-105", "KA01 IJ 7890", "Van", "Maruti", "Eeco", 2019, FuelType.PETROL, 12.9080, 77.5850, VehicleStatus.OFFLINE);
        Vehicle v6 = vehicle(cid, "VH-106", "KA01 KL 2345", "Truck", "Eicher", "Pro 2049", 2021, FuelType.DIESEL, 13.0450, 77.6200, VehicleStatus.MAINTENANCE);

        assign(cid, v1.id, d1.id);
        assign(cid, v2.id, d2.id);
        assign(cid, v3.id, d3.id);
        assign(cid, v4.id, d4.id);

        geofence(cid, "Bengaluru City Center", 12.9716, 77.5946, 4000);
        geofence(cid, "Kempegowda Airport", 13.1986, 77.7066, 3000);
        geofence(cid, "Whitefield Depot", 12.9698, 77.7500, 2500);

        trip(cid, v1.id, d1.id, "Bengaluru Warehouse", "Whitefield Depot", TripStatus.IN_PROGRESS, 24.5);
        trip(cid, v2.id, d2.id, "Indiranagar Hub", "Kempegowda Airport", TripStatus.IN_PROGRESS, 38.0);
        trip(cid, v3.id, d3.id, "MG Road", "Electronic City", TripStatus.PLANNED, 18.0);
        trip(cid, v5.id, null, "Jayanagar", "Koramangala", TripStatus.COMPLETED, 9.5);

        Customer cu1 = customer(cid, "FreshMart Retail", "Anita Desai", "+91 98800 11111");
        Customer cu2 = customer(cid, "MediCare Pharmacy", "Rahul Verma", "+91 98800 22222");
        Customer cu3 = customer(cid, "TechWorld Electronics", "Sneha Iyer", "+91 98800 33333");

        delivery(cid, cu1.id, v1.id, d1.id, "ORD-1001", "Bengaluru Warehouse", "Whitefield Depot", DeliveryStatus.IN_TRANSIT);
        delivery(cid, cu2.id, v2.id, d2.id, "ORD-1002", "Indiranagar Hub", "Kempegowda Airport", DeliveryStatus.IN_TRANSIT);
        delivery(cid, cu3.id, null, null, "ORD-1003", "MG Road", "Electronic City", DeliveryStatus.PENDING);

        MaintenanceRecord m = new MaintenanceRecord();
        m.companyId = cid;
        m.vehicleId = v6.id;
        m.type = MaintenanceType.ENGINE;
        m.status = "IN_PROGRESS";
        m.description = "Engine overhaul and brake pad replacement";
        m.vendor = "Bengaluru Truck Service";
        m.serviceDate = Instant.now().minus(2, ChronoUnit.DAYS);
        m.nextServiceDue = Instant.now().plus(20, ChronoUnit.DAYS);
        m.cost = 18500.0;
        m.odometer = 48230.0;
        m.workOrderNumber = "WO-2026-001";
        maintenance.save(m);

        FuelRecord f1 = new FuelRecord();
        f1.companyId = cid;
        f1.vehicleId = v1.id;
        f1.driverId = d1.id;
        f1.fuelType = FuelType.DIESEL;
        f1.quantity = 45.0;
        f1.cost = 4200.0;
        f1.odometer = 50120.0;
        f1.station = "Indian Oil, MG Road";
        fuel.save(f1);

        FuelRecord f2 = new FuelRecord();
        f2.companyId = cid;
        f2.vehicleId = v2.id;
        f2.driverId = d2.id;
        f2.fuelType = FuelType.DIESEL;
        f2.quantity = 30.0;
        f2.cost = 2800.0;
        f2.odometer = 33450.0;
        f2.station = "HP Petrol Pump, Airport Road";
        fuel.save(f2);

        document(cid, v1.id, "Insurance", "INS-778812", LocalDate.now().plus(25, ChronoUnit.DAYS));
        document(cid, v1.id, "Fitness Certificate", "FC-556677", LocalDate.now().plus(120, ChronoUnit.DAYS));
        document(cid, v2.id, "Permit", "PMT-991122", LocalDate.now().plus(10, ChronoUnit.DAYS));

        log.info("Seed complete. Login with admin/admin123, manager/manager123, dispatch/dispatch123, driver/driver123");
    }

    private void user(Long cid, String username, String password, String fullName, String email, Role role) {
        AppUser u = new AppUser();
        u.companyId = cid;
        u.username = username;
        u.password = encoder.encode(password);
        u.fullName = fullName;
        u.email = email;
        u.role = role;
        users.save(u);
    }

    private Driver driver(Long cid, String name, String phone, String license) {
        Driver d = new Driver();
        d.companyId = cid;
        d.fullName = name;
        d.phone = phone;
        d.licenseNumber = license;
        d.licenseExpiry = LocalDate.now().plus(2, ChronoUnit.YEARS);
        d.status = DriverStatus.AVAILABLE;
        d.safetyScore = 80 + (int) (Math.random() * 20);
        return drivers.save(d);
    }

    private Vehicle vehicle(Long cid, String code, String reg, String type, String make, String model, int year,
                            FuelType fuelType, double lat, double lng, VehicleStatus status) {
        Vehicle v = new Vehicle();
        v.companyId = cid;
        v.code = code;
        v.registrationNumber = reg;
        v.type = type;
        v.make = make;
        v.model = model;
        v.year = year;
        v.fuelType = fuelType;
        v.status = status;
        v.odometer = 10000 + Math.random() * 80000;
        if (simulatorEnabled) {
            // Synthetic demo positions - only used while the simulator is enabled.
            v.lastLat = lat;
            v.lastLng = lng;
            v.lastSpeed = status == VehicleStatus.MOVING ? 35.0 : 0.0;
            v.lastHeading = (int) (Math.random() * 360);
            v.lastIgnition = status == VehicleStatus.MOVING || status == VehicleStatus.IDLE;
            v.lastUpdate = Instant.now();
            v.confidence = LocationConfidence.HIGH;
            // Seed coordinates are synthetic demo data, not real device GPS.
            v.lastSource = LocationSource.SIMULATED;
        } else {
            // No genuine GPS yet - leave the position empty so the UI shows its empty state.
            v.lastLat = null;
            v.lastLng = null;
        }
        return vehicles.save(v);
    }

    private void assign(Long cid, Long vehicleId, Long driverId) {
        VehicleDriverAssignment a = new VehicleDriverAssignment();
        a.companyId = cid;
        a.vehicleId = vehicleId;
        a.driverId = driverId;
        a.active = true;
        assignments.save(a);
    }

    private void geofence(Long cid, String name, double lat, double lng, double radiusM) {
        Geofence g = new Geofence();
        g.companyId = cid;
        g.name = name;
        g.type = GeofenceType.CIRCLE;
        g.centerLat = lat;
        g.centerLng = lng;
        g.radiusM = radiusM;
        g.active = true;
        geofences.save(g);
    }

    private void trip(Long cid, Long vehicleId, Long driverId, String origin, String destination,
                     TripStatus status, double plannedKm) {
        Trip t = new Trip();
        t.companyId = cid;
        t.vehicleId = vehicleId;
        t.driverId = driverId;
        t.origin = origin;
        t.destination = destination;
        t.status = status;
        t.plannedDistanceKm = plannedKm;
        t.plannedStart = Instant.now();
        if (status == TripStatus.IN_PROGRESS) {
            t.actualStart = Instant.now().minus(30, ChronoUnit.MINUTES);
        } else if (status == TripStatus.COMPLETED) {
            t.actualStart = Instant.now().minus(2, ChronoUnit.HOURS);
            t.actualEnd = Instant.now().minus(1, ChronoUnit.HOURS);
            t.distanceKm = plannedKm;
        }
        trips.save(t);
    }

    private Customer customer(Long cid, String name, String contact, String phone) {
        Customer c = new Customer();
        c.companyId = cid;
        c.name = name;
        c.contactName = contact;
        c.phone = phone;
        c.trackingToken = UUID.randomUUID().toString().replace("-", "");
        return customers.save(c);
    }

    private void delivery(Long cid, Long customerId, Long vehicleId, Long driverId, String reference,
                         String pickup, String drop, DeliveryStatus status) {
        Delivery d = new Delivery();
        d.companyId = cid;
        d.customerId = customerId;
        d.vehicleId = vehicleId;
        d.driverId = driverId;
        d.reference = reference;
        d.pickupAddress = pickup;
        d.deliveryAddress = drop;
        d.status = status;
        d.scheduledAt = Instant.now().plus(2, ChronoUnit.HOURS);
        d.eta = Instant.now().plus(90, ChronoUnit.MINUTES);
        d.trackingToken = UUID.randomUUID().toString().replace("-", "");
        deliveries.save(d);
    }

    private void document(Long cid, Long vehicleId, String type, String number, LocalDate expiry) {
        VehicleDocument doc = new VehicleDocument();
        doc.companyId = cid;
        doc.vehicleId = vehicleId;
        doc.type = type;
        doc.number = number;
        doc.issuedDate = LocalDate.now().minus(1, ChronoUnit.YEARS);
        doc.expiryDate = expiry;
        documents.save(doc);
    }
}
