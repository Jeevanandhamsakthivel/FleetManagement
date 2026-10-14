package com.fleet.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fleet.domain.AppUser;
import com.fleet.domain.Company;
import com.fleet.domain.Role;
import com.fleet.repo.AppUserRepository;
import com.fleet.repo.CompanyRepository;
import com.fleet.security.CurrentUser;
import com.fleet.security.JwtService;
import com.fleet.service.AuditService;
import com.fleet.web.dto.AuthResponse;
import com.fleet.web.dto.LoginRequest;
import com.fleet.web.dto.RegisterRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository users;
    private final CompanyRepository companies;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;
    private final CurrentUser currentUser;

    public AuthController(AppUserRepository users, CompanyRepository companies, PasswordEncoder encoder,
                          JwtService jwt, AuditService audit, CurrentUser currentUser) {
        this.users = users;
        this.companies = companies;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
        this.currentUser = currentUser;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        AppUser u = users.findByUsername(req.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!u.enabled || req.password() == null || !encoder.matches(req.password(), u.password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        String token = jwt.generate(u.id, u.username, u.role.name(), u.companyId);
        Company c = u.companyId != null ? companies.findById(u.companyId).orElse(null) : null;
        audit.record(u.companyId, u.id, u.username, "LOGIN", "AppUser", u.id, "User logged in");
        return new AuthResponse(token, u.id, u.username, u.fullName, u.role.name(), u.companyId,
                c != null ? c.name : null);
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest req) {
        if (req.username() == null || req.password() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password are required");
        }
        if (users.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        Company c = new Company();
        c.name = req.companyName() != null ? req.companyName() : req.username() + " Fleet";
        c.email = req.email();
        c.phone = req.phone();
        c = companies.save(c);

        AppUser u = new AppUser();
        u.companyId = c.id;
        u.username = req.username();
        u.password = encoder.encode(req.password());
        u.fullName = req.fullName() != null ? req.fullName() : req.username();
        u.email = req.email();
        u.phone = req.phone();
        u.role = Role.COMPANY_ADMIN;
        u = users.save(u);

        String token = jwt.generate(u.id, u.username, u.role.name(), u.companyId);
        audit.record(c.id, u.id, u.username, "REGISTER", "Company", c.id, "Company self-registered");
        return new AuthResponse(token, u.id, u.username, u.fullName, u.role.name(), c.id, c.name);
    }

    @GetMapping("/me")
    public AuthResponse me() {
        JwtService.Claims claims = currentUser.get();
        if (claims == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        AppUser u = users.findById(claims.userId()).orElse(null);
        Company c = claims.companyId() != null ? companies.findById(claims.companyId()).orElse(null) : null;
        return new AuthResponse(null, claims.userId(), claims.username(), u != null ? u.fullName : null,
                claims.role(), claims.companyId(), c != null ? c.name : null);
    }
}
