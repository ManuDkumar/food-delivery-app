package dev.manu.food_delivery_app.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID itemId,
        String itemName,
        BigDecimal price,
        int quantity,
        BigDecimal lineTotal
) {
}
