package com.fleet.repo;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.TripLocation;

public interface TripLocationRepository extends JpaRepository<TripLocation, Long> {
    List<TripLocation> findAllByTripIdOrderByRecordedAtAsc(Long tripId);

    List<TripLocation> findTop100ByVehicleIdOrderByRecordedAtDesc(Long vehicleId);

    List<TripLocation> findAllByVehicleIdAndRecordedAtAfterOrderByRecordedAtAsc(Long vehicleId, Instant after);
}
