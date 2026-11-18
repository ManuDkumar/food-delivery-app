package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.entity.Restaurant;
import dev.manu.food_delivery_app.entity.User;
import dev.manu.food_delivery_app.enums.RestaurantStatus;
import dev.manu.food_delivery_app.enums.Role;
import dev.manu.food_delivery_app.enums.UserStatus;
import dev.manu.food_delivery_app.repository.OrderRepository;
import dev.manu.food_delivery_app.repository.RestaurantRepository;
import dev.manu.food_delivery_app.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private OrderRepository orderRepository;
    @InjectMocks
    private AdminService adminService;

    @BeforeEach
    void seedAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("Admin@gmail.com", null, List.of())
        );
    }
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }
    @Test
    void updateStatus_unknownUser_throw404() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> adminService.updateStatus(id, UserStatus.BLOCKED)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getReason()).isEqualTo("User not found: " + id);
    }

    @Test
    void updateStatus_blockOwner_closesRestaurants() {
        User owner = user("owner@test.com", Role.
                RESTAURANT_OWNER, UserStatus.ACTIVE);
        Restaurant open = restaurant(owner, RestaurantStatus.OPEN);

        when(userRepository.findById(open.getId())).thenReturn(Optional.of(owner));
        when(restaurantRepository.findByOwner(owner)).thenReturn(List.of(open));
        adminService.updateStatus(open.getId(), UserStatus.BLOCKED);

        assertThat(owner.getUserStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(open.getStatus()).isEqualTo(RestaurantStatus.CLOSED);
        verify(restaurantRepository, times(1)).findByOwner(owner);
    }

    @Test
    void selfBlocked_throws409() {
        User admin = user("Admin@gmail.com", Role.ADMIN, UserStatus.ACTIVE);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> adminService.updateStatus(admin.getId(), UserStatus.BLOCKED));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Admin can not change status to " + UserStatus.BLOCKED);
    }

    @Test
    void sameStatus_skips() {
        User user = user("user@test.com", Role.CUSTOMER, UserStatus.ACTIVE);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        adminService.updateStatus(user.getId(), UserStatus.ACTIVE);

        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);

        verify(userRepository, never()).save(user);
    }

    @Test
    void unblockOwner_doesNotReopen() {
        User owner = user("owner@test.com", Role.RESTAURANT_OWNER, UserStatus.BLOCKED);
        Restaurant closed  = restaurant(owner, RestaurantStatus.CLOSED);

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        adminService.updateStatus(owner.getId(), UserStatus.ACTIVE);

        assertThat(owner.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(closed.getStatus()).isEqualTo(RestaurantStatus.CLOSED);

        verify(restaurantRepository, never()).findByOwner(owner);
    }

    @Test
    void blockCustomer_noCascade() {
        User customer = user("customer",  Role.CUSTOMER, UserStatus.ACTIVE);
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        adminService.updateStatus(customer.getId(), UserStatus.BLOCKED);

        assertThat(customer.getUserStatus()).isEqualTo(UserStatus.BLOCKED);

        verify(restaurantRepository, never()).findByOwner(customer);
    }

    private User user(String email, Role role, UserStatus
            status) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .role(role)
                .userStatus(status)
                .build();
    }

    private Restaurant restaurant(User owner, RestaurantStatus
            status) {
        return Restaurant.builder()
                .id(UUID.randomUUID())
                .name("Test Restaurant")
                .address("Test Street 1")
                .status(status)
                .owner(owner)
                .build();
    }
}
