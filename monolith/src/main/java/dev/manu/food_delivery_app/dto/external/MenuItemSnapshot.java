package dev.manu.food_delivery_app.dto.external;

import  java.util.UUID;
import java.math.BigDecimal;

public record MenuItemSnapshot(
        UUID id,
        String name,
        BigDecimal price,
        boolean veg,
        boolean available
) {}