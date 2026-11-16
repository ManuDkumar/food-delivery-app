package dev.manu.food_delivery_app.repository;

import dev.manu.food_delivery_app.entity.Order;
import dev.manu.food_delivery_app.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    @Query("SELECT o FROM Order o JOIN FETCH o.restaurant WHERE o.customer = :customer ORDER BY o.createdAt DESC")
    Page<Order> findMyOrders(@Param("customer") User customer, Pageable pageable);

    List<Order> findByRestaurant_OwnerOrderByCreatedAtDesc(User owner);
    List<Order> findAllByOrderByCreatedAtDesc();

    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems JOIN FETCH o.restaurant WHERE o.customer = :customer ORDER BY o.createdAt DESC")
    List<Order> findMyOrdersWithItems(@Param("customer") User customer);
}
