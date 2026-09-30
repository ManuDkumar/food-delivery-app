package dev.manu.food_delivery_app.control;

import dev.manu.food_delivery_app.dto.AuthResponse;
import dev.manu.food_delivery_app.dto.LoginRequest;
import dev.manu.food_delivery_app.dto.RegisterRequest;
import dev.manu.food_delivery_app.service.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/customer/register")
    public ResponseEntity<AuthResponse> customerRegister(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.status(201).body(authService.customerRegister(registerRequest));
    }

    @PostMapping("/restaurant/register")
    public ResponseEntity<AuthResponse> restaurantRegister(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.status(201).body(authService.restaurantRegister(registerRequest));
    }

    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> aminRegister(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.status(201).body(authService.registerAdmin(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> userLogin(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }
}
