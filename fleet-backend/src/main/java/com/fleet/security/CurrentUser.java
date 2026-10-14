package com.fleet.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Convenience accessor for the authenticated principal (JWT claims). */
@Component
public class CurrentUser {

    public JwtService.Claims get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtService.Claims claims) {
            return claims;
        }
        return null;
    }

    public Long companyId() {
        JwtService.Claims c = get();
        return c == null ? null : c.companyId();
    }

    public Long userId() {
        JwtService.Claims c = get();
        return c == null ? null : c.userId();
    }
}
