package com.fleet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers", indexes = @Index(name = "idx_customers_company", columnList = "company_id"))
public class Customer extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    public Long companyId;

    @Column(nullable = false)
    public String name;

    @Column(name = "contact_name")
    public String contactName;

    public String email;
    public String phone;
    public String address;

    /** Public tracking token for customer-facing delivery tracking. */
    @Column(name = "tracking_token", unique = true)
    public String trackingToken;
}
