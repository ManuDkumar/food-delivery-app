package dev.manu.user_service.dto.admin;

import dev.manu.user_service.enums.Role;
import dev.manu.user_service.enums.UserStatus;

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
