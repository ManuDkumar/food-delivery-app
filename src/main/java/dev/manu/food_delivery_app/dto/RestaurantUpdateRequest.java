package dev.manu.food_delivery_app.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RestaurantUpdateRequest(
        @Size(min = 3,  max = 50,
                message = "Name must be minimum 3 letter and maximum 50 letters")
        String name,

        @Size(max = 500,
                message = "Description should be within 500 characters")
        String description,

        @Size(min = 3,  max = 100,
                message = "Address must be minimum 3 letter and maximum 100 letters")
        String address,

        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number")
        String phone
) {
}
