package com.fleet.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fleet.domain.Geofence;

public interface GeofenceRepository extends JpaRepository<Geofence, Long> {
    List<Geofence> findAllByCompanyId(Long companyId);

    List<Geofence> findAllByCompanyIdAndActiveTrue(Long companyId);

    List<Geofence> findAllByActiveTrue();
}
