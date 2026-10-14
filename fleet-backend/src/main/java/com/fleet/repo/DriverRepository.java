package com.fleet.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.Driver;
import com.fleet.domain.DriverStatus;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findAllByCompanyId(Long companyId);

    long countByCompanyId(Long companyId);

    long countByCompanyIdAndStatus(Long companyId, DriverStatus status);
}
