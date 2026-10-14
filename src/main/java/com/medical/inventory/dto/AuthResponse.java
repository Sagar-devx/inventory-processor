package com.medical.inventory.dto;

public record AuthResponse(
    String token,
    String type,
    String username,
    String role,
    long expiresInMs
) {
    public static AuthResponse of(String token, String username, String role, long expiresInMs) {
        return new AuthResponse(token, "Bearer", username, role, expiresInMs);
    }
}
