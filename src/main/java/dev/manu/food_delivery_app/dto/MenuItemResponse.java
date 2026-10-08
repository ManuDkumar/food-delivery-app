package dev.manu.food_delivery_app.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        boolean veg,
        boolean available,
        Date createdAt,
        Date updatedAt
) {
}
