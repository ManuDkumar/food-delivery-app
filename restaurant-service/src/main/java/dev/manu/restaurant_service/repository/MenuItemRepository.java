package dev.manu.restaurant_service.repository;

import dev.manu.restaurant_service.entity.MenuItem;
import dev.manu.restaurant_service.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MenuItemRepository  extends JpaRepository<MenuItem, UUID> {
    List<MenuItem> findByRestaurantAndActiveTrueOrderByCreatedAtDesc(Restaurant restaurant);
}
