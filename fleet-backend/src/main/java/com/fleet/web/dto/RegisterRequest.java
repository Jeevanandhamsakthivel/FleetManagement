package com.fleet.web.dto;

public record RegisterRequest(String companyName, String username, String password, String fullName,
                              String email, String phone) {
}
