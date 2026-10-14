package com.fleet.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.VehicleDocument;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {
    List<VehicleDocument> findAllByCompanyIdOrderByExpiryDateAsc(Long companyId);

    List<VehicleDocument> findAllByExpiryDateBeforeAndExpiryAlertSentFalse(LocalDate date);
}
