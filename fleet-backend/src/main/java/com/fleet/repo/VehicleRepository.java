package com.fleet.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.Vehicle;
import com.fleet.domain.VehicleStatus;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findAllByCompanyId(Long companyId);

    Optional<Vehicle> findByCode(String code);

    Optional<Vehicle> findByCompanyIdAndCode(Long companyId, String code);

    long countByCompanyId(Long companyId);

    long countByCompanyIdAndStatus(Long companyId, VehicleStatus status);
}
