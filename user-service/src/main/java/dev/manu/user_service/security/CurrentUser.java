package dev.manu.user_service.security;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public record CurrentUser(UUID userId, String email, String role) {

    public static CurrentUser from(Claims claims) {
        String userId = claims.get("userId", String.class);
        String role = claims.get("role", String.class);

        if (userId == null || role == null) {
            throw new IllegalArgumentException("Token is missing required claims (userId/role)");
        }
        return new CurrentUser(
                UUID.fromString(userId),
                claims.getSubject(),
                role
        );
    }

    public static CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUser current) {
            return current;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean owns(UUID ownerId) {
        return userId.equals(ownerId);
    }
}
