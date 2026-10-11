package com.nikhil.patientservice.dto;

public record LoginResponse(String token, String username, String role, long expiresInSeconds) {
}
