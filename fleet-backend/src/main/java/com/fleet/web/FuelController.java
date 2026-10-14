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

import com.fleet.domain.FuelRecord;
import com.fleet.repo.FuelRecordRepository;
import com.fleet.security.CurrentUser;

@RestController
@RequestMapping("/api/fuel")
public class FuelController {

    private final FuelRecordRepository fuel;
    private final CurrentUser currentUser;

    public FuelController(FuelRecordRepository fuel, CurrentUser currentUser) {
        this.fuel = fuel;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<FuelRecord> list() {
        return fuel.findAllByCompanyIdOrderByFilledAtDesc(currentUser.companyId());
    }

    @PostMapping
    public FuelRecord create(@RequestBody FuelRecord body) {
        body.id = null;
        body.companyId = currentUser.companyId();
        body.createdAt = null;
        if (body.vehicleId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vehicleId is required");
        }
        return fuel.save(body);
    }

    @PutMapping("/{id}")
    public FuelRecord update(@PathVariable Long id, @RequestBody FuelRecord body) {
        FuelRecord f = require(id);
        body.id = f.id;
        body.companyId = f.companyId;
        body.createdAt = f.createdAt;
        if (body.vehicleId == null) {
            body.vehicleId = f.vehicleId;
        }
        return fuel.save(body);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        fuel.delete(require(id));
    }

    private FuelRecord require(Long id) {
        FuelRecord f = fuel.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));
        if (!f.companyId.equals(currentUser.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your record");
        }
        return f;
    }
}
