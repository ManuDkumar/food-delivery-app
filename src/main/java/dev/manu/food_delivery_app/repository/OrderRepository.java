package dev.manu.food_delivery_app.repository;

import dev.manu.food_delivery_app.entity.Order;
import dev.manu.food_delivery_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerOrderByCreatedAtDesc(User customer);
    List<Order> findByRestaurant_OwnerOrderByCreatedAtDesc(User owner);
    List<Order> findAllByOrderByCreatedAtDesc();
}
