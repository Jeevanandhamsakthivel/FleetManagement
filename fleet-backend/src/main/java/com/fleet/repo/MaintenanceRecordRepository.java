package com.fleet.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.MaintenanceRecord;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {
    List<MaintenanceRecord> findAllByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<MaintenanceRecord> findAllByVehicleIdOrderByServiceDateDesc(Long vehicleId);

    List<MaintenanceRecord> findAllByCompanyIdAndStatus(Long companyId, String status);
}
