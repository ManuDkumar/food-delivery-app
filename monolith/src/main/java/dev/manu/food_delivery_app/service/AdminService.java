package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.dto.admin.AdminOrderResponse;
import dev.manu.food_delivery_app.dto.admin.AdminRestaurantResponse;
import dev.manu.food_delivery_app.dto.order.PageResponse;
import dev.manu.food_delivery_app.entity.Order;
import dev.manu.food_delivery_app.entity.Restaurant;
import dev.manu.food_delivery_app.repository.OrderRepository;
import dev.manu.food_delivery_app.repository.RestaurantRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class AdminService {
    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;

    private AdminRestaurantResponse  toAdminRestaurantResponse(Restaurant restaurant) {
        return new AdminRestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.getStatus(),
                restaurant.getOwner().getId(),
                restaurant.getOwner().getName(),
                restaurant.getOwner().getEmail(),
                restaurant.getOwner().getPhone(),
                restaurant.getCreatedAt(),
                restaurant.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminRestaurantResponse> getAllRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(this::toAdminRestaurantResponse).toList();
    }

    private AdminOrderResponse toAdminOrderResponse(Order order) {
        return new AdminOrderResponse(
                order.getId(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderResponse> getAllOrders(Pageable pageable) {
        Page<Order> page = orderRepository.findAdminOrders(pageable);
        List<AdminOrderResponse> content = page.getContent().stream()
                .map(this::toAdminOrderResponse).toList();
        return PageResponse.from(page, content);
    }
}
