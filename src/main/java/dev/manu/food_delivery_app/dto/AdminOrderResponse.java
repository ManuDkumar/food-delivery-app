package dev.manu.food_delivery_app.dto;

import dev.manu.food_delivery_app.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public record AdminOrderResponse(
        UUID id,
        UUID restaurantId,
        String restaurantName,
        String customerName,
        String customerEmail,
        OrderStatus status,
        BigDecimal totalAmount,
        Date createdAt
) {
}
