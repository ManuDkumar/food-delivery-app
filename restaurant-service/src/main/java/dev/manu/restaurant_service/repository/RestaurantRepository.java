package dev.manu.restaurant_service.repository;

import dev.manu.restaurant_service.entity.Restaurant;
import dev.manu.restaurant_service.enums.RestaurantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    List<Restaurant> findByStatusNot(RestaurantStatus status);
    List<Restaurant> findByOwnerId(UUID ownerId);
}
