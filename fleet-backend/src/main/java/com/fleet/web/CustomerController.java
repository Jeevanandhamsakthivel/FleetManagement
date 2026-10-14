package com.fleet.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fleet.domain.Customer;
import com.fleet.repo.CustomerRepository;
import com.fleet.security.CurrentUser;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customers;
    private final CurrentUser currentUser;

    public CustomerController(CustomerRepository customers, CurrentUser currentUser) {
        this.customers = customers;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Customer> list() {
        return customers.findAllByCompanyId(currentUser.companyId());
    }

    @PostMapping
    public Customer create(@RequestBody Customer body) {
        body.id = null;
        body.companyId = currentUser.companyId();
        body.createdAt = null;
        if (body.trackingToken == null) {
            body.trackingToken = java.util.UUID.randomUUID().toString().replace("-", "");
        }
        return customers.save(body);
    }

    @PutMapping("/{id}")
    public Customer update(@PathVariable Long id, @RequestBody Customer body) {
        Customer c = require(id);
        body.id = c.id;
        body.companyId = c.companyId;
        body.createdAt = c.createdAt;
        if (body.trackingToken == null) {
            body.trackingToken = c.trackingToken;
        }
        return customers.save(body);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        customers.delete(require(id));
    }

    private Customer require(Long id) {
        Customer c = customers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        if (!c.companyId.equals(currentUser.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your customer");
        }
        return c;
    }
}
