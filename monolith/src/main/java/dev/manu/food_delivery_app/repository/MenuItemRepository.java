package dev.manu.food_delivery_app.repository;

import dev.manu.food_delivery_app.entity.MenuItem;
import dev.manu.food_delivery_app.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MenuItemRepository  extends JpaRepository<MenuItem, UUID> {
    List<MenuItem> findByRestaurantAndActiveTrueOrderByCreatedAtDesc(Restaurant restaurant);
}
