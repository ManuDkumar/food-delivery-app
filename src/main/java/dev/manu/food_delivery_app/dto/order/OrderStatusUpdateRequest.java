package dev.manu.food_delivery_app.dto.order;

import dev.manu.food_delivery_app.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull OrderStatus status
        ) {
}
