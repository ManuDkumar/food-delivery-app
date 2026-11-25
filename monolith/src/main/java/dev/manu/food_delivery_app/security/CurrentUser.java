package dev.manu.food_delivery_app.security;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public record CurrentUser(UUID userId, String email, String name, String role) {
    public static CurrentUser from(Claims claims) {
        String userId = claims.get("userId", String.class);
        String email = claims.getSubject();
        String name = claims.get("name", String.class);
        String role = claims.get("role", String.class);

        if (userId == null || role == null) {
            throw new IllegalArgumentException("Token is missing required claims (userId/role)");
        }
        return  new CurrentUser(UUID.fromString(userId), email, name != null ? name : email, role);
    }

    public static CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof CurrentUser current) {
            return current;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
