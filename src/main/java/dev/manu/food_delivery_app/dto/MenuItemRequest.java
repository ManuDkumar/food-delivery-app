package dev.manu.food_delivery_app.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MenuItemRequest(
        @NotBlank
        @Size(min = 3, max = 100)
        String name,

        @Size(max = 500)
        String description,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal price,

        boolean veg
) {
}
