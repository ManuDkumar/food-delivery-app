package dev.manu.user_service.controller;

import dev.manu.user_service.dto.user.ProfileResponse;
import dev.manu.user_service.dto.user.ProfileUpdateRequest;
import dev.manu.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@AllArgsConstructor
public class UserController {

    private UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getUserProfile() {
        return ResponseEntity.status(200).body(userService.getProfile());
    }


    @PatchMapping("/update/profile")
    public ResponseEntity<ProfileResponse> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest profileUpdateRequest) {
        return ResponseEntity.status(200).body(userService.updateProfile(profileUpdateRequest));
    }


    @DeleteMapping("/delete/profile")
    public ResponseEntity<Void> deleteProfile()  {
        userService.deleteProfile();
        return ResponseEntity.status(204).build();
    }

}
