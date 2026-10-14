package com.fleet.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.FuelRecord;

public interface FuelRecordRepository extends JpaRepository<FuelRecord, Long> {
    List<FuelRecord> findAllByCompanyIdOrderByFilledAtDesc(Long companyId);

    List<FuelRecord> findAllByVehicleIdOrderByFilledAtDesc(Long vehicleId);
}
