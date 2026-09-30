package dev.manu.food_delivery_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required.")
        @Size(min = 2, max = 100, message = "Enter a valid name.")
        String name,

        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email format.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, message = "Password must be at least 8 characters.")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).+$",
                message = "Password must contain at least 1 uppercase letter, 1 digit and 1 special character"
        )
        String password,

        @NotBlank(message = "Phone number is required.")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number")
        String phone
) {
}
