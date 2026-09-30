package dev.manu.food_delivery_app.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(min = 2, max = 100, message = "Enter a valid name.")
        String name,

        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number")
        String phone) {
}
