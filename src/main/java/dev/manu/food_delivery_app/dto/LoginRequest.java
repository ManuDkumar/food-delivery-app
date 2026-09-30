package dev.manu.food_delivery_app.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email missing.")
        @Email(message = "Enter valid email format.")
        String email,
        @NotBlank(message = "Password missing.")
        String password) {
}
