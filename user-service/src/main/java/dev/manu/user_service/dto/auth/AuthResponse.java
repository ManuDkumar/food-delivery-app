package dev.manu.user_service.dto.auth;

public record AuthResponse(
        String token, String name, String email, String role
) {
}
