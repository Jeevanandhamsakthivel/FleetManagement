package com.fleet.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fleet.domain.AppUser;
import com.fleet.domain.Role;
import com.fleet.repo.AppUserRepository;
import com.fleet.security.CurrentUser;
import com.fleet.service.AuditService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public UserController(AppUserRepository users, PasswordEncoder encoder, CurrentUser currentUser, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @GetMapping
    public List<AppUser> list() {
        return users.findAllByCompanyId(currentUser.companyId()).stream().peek(u -> u.password = null).toList();
    }

    @PostMapping
    public AppUser create(@RequestBody AppUser body) {
        if (body.username == null || body.password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password are required");
        }
        if (users.existsByUsername(body.username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        body.id = null;
        body.companyId = currentUser.companyId();
        body.createdAt = null;
        body.password = encoder.encode(body.password);
        if (body.role == null) {
            body.role = Role.FLEET_MANAGER;
        }
        AppUser saved = users.save(body);
        audit.record(saved.companyId, currentUser.userId(), null, "CREATE", "AppUser", saved.id, "Created user " + saved.username);
        saved.password = null;
        return saved;
    }

    @PutMapping("/{id}")
    public AppUser update(@PathVariable Long id, @RequestBody AppUser body) {
        AppUser u = require(id);
        u.fullName = body.fullName;
        u.email = body.email;
        u.phone = body.phone;
        if (body.role != null) {
            u.role = body.role;
        }
        u.enabled = body.enabled;
        if (body.password != null && !body.password.isBlank()) {
            u.password = encoder.encode(body.password);
        }
        AppUser saved = users.save(u);
        audit.record(saved.companyId, currentUser.userId(), null, "UPDATE", "AppUser", saved.id, "Updated user " + saved.username);
        saved.password = null;
        return saved;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        AppUser u = require(id);
        users.delete(u);
        audit.record(u.companyId, currentUser.userId(), null, "DELETE", "AppUser", id, "Deleted user " + u.username);
    }

    private AppUser require(Long id) {
        AppUser u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!u.companyId.equals(currentUser.companyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your user");
        }
        return u;
    }
}
