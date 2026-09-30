package dev.manu.food_delivery_app.control;

import dev.manu.food_delivery_app.enums.UserStatus;
import dev.manu.food_delivery_app.service.AdminService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@AllArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PatchMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID userId,
                                       @RequestBody UserStatus userStatus) {

        adminService.updateStatus(userId, userStatus);
        return ResponseEntity.ok().build();
    }
}
