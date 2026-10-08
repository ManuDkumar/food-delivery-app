package dev.manu.food_delivery_app.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MenuItemUpdateRequest(
        @Size(min = 3, max = 100)
        String name,

        @Size(max = 500)
        String description,

        @DecimalMin("0.01")
        BigDecimal price,

        Boolean veg
) {
}
