package com.fleet.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.ProofOfDelivery;

public interface ProofOfDeliveryRepository extends JpaRepository<ProofOfDelivery, Long> {
    List<ProofOfDelivery> findAllByDeliveryId(Long deliveryId);
}
