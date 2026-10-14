package com.fleet.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Company findByName(String name);
}
