package com.fleet.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findAllByCompanyId(Long companyId);

    Optional<Customer> findByTrackingToken(String trackingToken);
}
