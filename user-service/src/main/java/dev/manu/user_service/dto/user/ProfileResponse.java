package dev.manu.user_service.dto.user;

public record ProfileResponse(
        String name, String email, String phone, String role) {
}
