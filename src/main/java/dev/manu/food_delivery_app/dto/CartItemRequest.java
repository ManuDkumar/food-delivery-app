package dev.manu.food_delivery_app.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record CartItemRequest(
        @NotNull
        UUID menuItemId,

        @NotNull
        @Min(1)
        @Max(100)
        Integer quantity
) {
}
