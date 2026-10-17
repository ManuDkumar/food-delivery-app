package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.dto.restaurant.PublicRestaurantResponse;
import dev.manu.food_delivery_app.dto.restaurant.RestaurantUpdateRequest;
import dev.manu.food_delivery_app.entity.Restaurant;
import dev.manu.food_delivery_app.entity.User;
import dev.manu.food_delivery_app.enums.RestaurantStatus;
import dev.manu.food_delivery_app.enums.Role;
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

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RestaurantServiceTest {
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private RestaurantService restaurantService;

    @BeforeEach
    void seedCustomer() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer@test.com", null, List.of())
        );
    }
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private User user(String email, Role role) {
        return User.builder().email(email).role(role).createdAt(new Date()).id(UUID.randomUUID()).build();
    }

    private Restaurant restaurant(RestaurantStatus
                                          status) {
        return Restaurant.builder()
                .id(UUID.randomUUID())
                .name("Test Restaurant")
                .address("Test Street 1")
                .owner(user("owner@Test.com", Role.RESTAURANT_OWNER))
                .status(status)
                .build();
    }

    @Test
    void getRestaurant_removedRestaurant_throw409() {
        Restaurant restaurant = restaurant(RestaurantStatus.REMOVED);

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> restaurantService.getRestaurant(restaurant.getId()));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Restaurant is not available at this address.");
    }
    @Test
    void updateRestaurant_notOwner_throw403() {
        User notOwner = user("customer@test.com", Role.CUSTOMER);
        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);
        RestaurantUpdateRequest request = new RestaurantUpdateRequest(restaurant.getName(), null, restaurant.getAddress(), "95957685");

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(notOwner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> restaurantService.updateRestaurant(restaurant.getId(), request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("You are not allowed to perform this action.");
    }
    @Test
    void updateRestaurant_patchUpdate_nullFieldsIgnored() {

        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);
        RestaurantUpdateRequest request = new RestaurantUpdateRequest("New Name", null, restaurant.getAddress(), null);
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(restaurant.getOwner()));
        when(restaurantRepository.saveAndFlush(restaurant)).thenAnswer(i -> i.getArgument(0));

        PublicRestaurantResponse result = restaurantService.updateRestaurant(restaurant.getId(), request);

        assertThat(result.restaurantName()).isEqualTo("New Name");
        assertThat(result.address()).isEqualTo(restaurant.getAddress());
        assertThat(result.status()).isEqualTo(RestaurantStatus.OPEN);
    }
}
