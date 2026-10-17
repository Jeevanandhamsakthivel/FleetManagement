package com.fleet.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.Trip;
import com.fleet.domain.TripStatus;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findAllByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<Trip> findAllByCompanyIdAndStatus(Long companyId, TripStatus status);

    List<Trip> findAllByStatus(TripStatus status);

    Optional<Trip> findFirstByVehicleIdAndStatus(Long vehicleId, TripStatus status);

    /** Newest in-progress trip for a vehicle (a vehicle may have stale active rows from older data). */
    Optional<Trip> findFirstByVehicleIdAndStatusOrderByIdDesc(Long vehicleId, TripStatus status);

    long countByCompanyIdAndStatus(Long companyId, TripStatus status);
}
