package dev.manu.food_delivery_app.dto.auth;

public record AuthResponse(
        String token, String name, String email, String role
) {
}
