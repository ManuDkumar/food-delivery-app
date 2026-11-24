package dev.manu.restaurant_service.dto.restaurant;

import dev.manu.restaurant_service.enums.RestaurantStatus;

import java.util.Date;
import java.util.UUID;

public record PublicRestaurantResponse(
        UUID id,
        String restaurantName,
        String description,
        String address,
        String phoneNumber,
        RestaurantStatus status,
        Date createdAt,
        Date lastUpdated
) {
}
