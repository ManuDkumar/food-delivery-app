package dev.manu.food_delivery_app.dto.order;

import dev.manu.food_delivery_app.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID restaurantId,
        String restaurantName,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        Date createdAt
) {
}
