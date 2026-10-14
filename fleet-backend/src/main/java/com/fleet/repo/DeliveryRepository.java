package com.fleet.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.Delivery;
import com.fleet.domain.DeliveryStatus;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findAllByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<Delivery> findAllByCompanyIdAndStatus(Long companyId, DeliveryStatus status);

    Optional<Delivery> findByTrackingToken(String trackingToken);

    long countByCompanyIdAndStatus(Long companyId, DeliveryStatus status);
}
