package com.fleet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class AppUser extends BaseEntity {

    @Column(name = "company_id")
    public Long companyId;

    @Column(nullable = false, unique = true)
    public String username;

    @Column(nullable = false)
    public String password;

    @Column(name = "full_name")
    public String fullName;

    public String email;
    public String phone;

    @Enumerated(EnumType.STRING)
    public Role role = Role.FLEET_MANAGER;

    public boolean enabled = true;
}
