package dev.manu.user_service.controller;

import dev.manu.user_service.dto.admin.AdminUserResponse;
import dev.manu.user_service.enums.UserStatus;
import dev.manu.user_service.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public List<AdminUserResponse> getAllUsers() {
        return adminService.getAllUsers();
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID userId, @RequestBody UserStatus userStatus) {
        adminService.updateStatus(userId, userStatus);
        return ResponseEntity.ok().build();
    }
}
