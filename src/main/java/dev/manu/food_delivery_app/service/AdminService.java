package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.dto.AdminOrderResponse;
import dev.manu.food_delivery_app.dto.AdminRestaurantResponse;
import dev.manu.food_delivery_app.dto.AdminUserResponse;
import dev.manu.food_delivery_app.entity.Order;
import dev.manu.food_delivery_app.entity.Restaurant;
import dev.manu.food_delivery_app.entity.User;
import dev.manu.food_delivery_app.enums.UserStatus;
import dev.manu.food_delivery_app.repository.OrderRepository;
import dev.manu.food_delivery_app.repository.RestaurantRepository;
import dev.manu.food_delivery_app.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;


    @Transactional
    public void updateStatus(UUID userId, UserStatus userStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + userId));

        String currentUser = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        if (user.getEmail().equals(currentUser) && userStatus != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin can not change status to " + userStatus);
        }

        if (user.getUserStatus().equals(userStatus)) {
            return;
        }
        user.setUserStatus(userStatus);
        userRepository.save(user);
    }
    private AdminUserResponse toAdminUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getUserStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toAdminUserResponse).toList();
    }

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
                order.getCustomer().getName(),
                order.getCustomer().getEmail(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminOrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toAdminOrderResponse).toList();
    }
}
