package dev.manu.food_delivery_app.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        String name,
        BigDecimal price,
        int quantity,
        BigDecimal lineTotal
) {
}
