package dev.manu.food_delivery_app.dto;

import dev.manu.food_delivery_app.enums.Role;
import dev.manu.food_delivery_app.enums.UserStatus;

import java.util.Date;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String name,
        String email,
        String phone,
        Role role,
        UserStatus status,
        Date createdAt,
        Date lastUpdated
) {
}
