package dev.manu.food_delivery_app.repository;

import dev.manu.food_delivery_app.entity.CartItem;
import dev.manu.food_delivery_app.entity.MenuItem;
import dev.manu.food_delivery_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByUserOrderByCreatedAtAsc(User user);
    Optional<CartItem> findByUserAndMenuItem(User user, MenuItem menuItem);
}
