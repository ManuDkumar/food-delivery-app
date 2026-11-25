package dev.manu.food_delivery_app.repository;

import dev.manu.food_delivery_app.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    // sort is owned by the Pageable (controller @PageableDefault) - no ORDER BY here
    @Query("SELECT o FROM Order o JOIN FETCH o.restaurant WHERE o.customerId = :customerId")
    Page<Order> findMyOrders(@Param("customerId") UUID customerId, Pageable pageable);

    // owner order list: paged, to-one fetch joins only (collection fetch + Pageable would paginate in memory)
    @Query("SELECT o FROM Order o JOIN FETCH o.restaurant WHERE o.restaurant.owner.id = :ownerId")
    Page<Order> findOwnerOrders(@Param("ownerId") UUID ownerId, Pageable pageable);

    // admin order list: needs customer + restaurant (to-one) but NO items -> single query, no items round-trip
    @Query("SELECT o FROM Order o JOIN FETCH o.restaurant")
    Page<Order> findAdminOrders(Pageable pageable);

}
