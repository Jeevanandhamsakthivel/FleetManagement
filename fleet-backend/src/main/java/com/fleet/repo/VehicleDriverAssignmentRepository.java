package com.fleet.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.VehicleDriverAssignment;

public interface VehicleDriverAssignmentRepository extends JpaRepository<VehicleDriverAssignment, Long> {
    List<VehicleDriverAssignment> findAllByCompanyId(Long companyId);

    Optional<VehicleDriverAssignment> findFirstByVehicleIdAndActiveTrue(Long vehicleId);

    Optional<VehicleDriverAssignment> findFirstByDriverIdAndActiveTrue(Long driverId);

    List<VehicleDriverAssignment> findAllByActiveTrue();
}
