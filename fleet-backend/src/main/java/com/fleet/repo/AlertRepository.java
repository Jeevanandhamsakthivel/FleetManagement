package com.fleet.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.Alert;
import com.fleet.domain.AlertType;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findAllByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<Alert> findAllByCompanyIdAndAcknowledgedFalseOrderByCreatedAtDesc(Long companyId);

    long countByCompanyIdAndAcknowledgedFalse(Long companyId);

    List<Alert> findTop50ByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<Alert> findByVehicleIdAndTypeAndAcknowledgedFalse(Long vehicleId, AlertType type);
}
