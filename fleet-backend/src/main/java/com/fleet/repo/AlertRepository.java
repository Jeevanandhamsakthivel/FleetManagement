package com.fleet.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.Alert;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findAllByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<Alert> findAllByCompanyIdAndAcknowledgedFalseOrderByCreatedAtDesc(Long companyId);

    long countByCompanyIdAndAcknowledgedFalse(Long companyId);

    List<Alert> findTop50ByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
