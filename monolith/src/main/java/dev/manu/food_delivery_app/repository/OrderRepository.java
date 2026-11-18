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
    // sort is owned by the Pageable (controller @PageableDefault) - no ORDER BY here
    @Query("SELECT o FROM Order o JOIN FETCH o.restaurant WHERE o.customer = :customer")
    Page<Order> findMyOrders(@Param("customer") User customer, Pageable pageable);

    // owner order list: paged, to-one fetch joins only (collection fetch + Pageable would paginate in memory)
    @Query("SELECT o FROM Order o JOIN FETCH o.restaurant WHERE o.restaurant.owner = :owner")
    Page<Order> findOwnerOrders(@Param("owner") User owner, Pageable pageable);

    // admin order list: needs customer + restaurant (to-one) but NO items -> single query, no items round-trip
    @Query("SELECT o FROM Order o JOIN FETCH o.restaurant JOIN FETCH o.customer")
    Page<Order> findAdminOrders(Pageable pageable);

}
