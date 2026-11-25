package dev.manu.food_delivery_app.repository;

import dev.manu.food_delivery_app.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByUserIdOrderByCreatedAtAsc(UUID userId);

    @Query("SELECT c FROM CartItem c JOIN FETCH c.menuItem m JOIN FETCH m.restaurant " + "WHERE c.userId = :userId ORDER BY c.createdAt ASC")
    List<CartItem> findCartWithMenuItems(@Param("userId") UUID userId);
}
