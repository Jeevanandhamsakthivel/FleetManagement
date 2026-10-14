package com.fleet.web.dto;

public record AuthResponse(String token, Long userId, String username, String fullName, String role,
                          Long companyId, String companyName) {
}
