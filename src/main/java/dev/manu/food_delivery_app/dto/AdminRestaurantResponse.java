package dev.manu.food_delivery_app.dto;

import dev.manu.food_delivery_app.enums.RestaurantStatus;

import java.util.Date;
import java.util.UUID;

public record AdminRestaurantResponse(
        UUID id,
        String restaurantName,
        String description,
        String address,
        String phoneNumber,
        RestaurantStatus status,
        UUID ownerId,
        String ownerName,
        String ownerEmail,
        String ownerPhone,
        Date createdAt,
        Date lastUpdated
) {
}
