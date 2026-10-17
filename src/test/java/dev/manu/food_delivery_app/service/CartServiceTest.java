package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.dto.cart.CartItemRequest;
import dev.manu.food_delivery_app.dto.cart.CartQuantityUpdateRequest;
import dev.manu.food_delivery_app.dto.cart.CartResponse;
import dev.manu.food_delivery_app.entity.CartItem;
import dev.manu.food_delivery_app.entity.MenuItem;
import dev.manu.food_delivery_app.entity.Restaurant;
import dev.manu.food_delivery_app.entity.User;
import dev.manu.food_delivery_app.enums.RestaurantStatus;
import dev.manu.food_delivery_app.enums.Role;
import dev.manu.food_delivery_app.enums.UserStatus;
import dev.manu.food_delivery_app.repository.CartItemRepository;
import dev.manu.food_delivery_app.repository.MenuItemRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class CartServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @InjectMocks
    private CartService cartService;

    private MenuItem menuItem(
            UUID id, Restaurant restaurant, BigDecimal price, boolean active, boolean available) {
        return MenuItem.builder().id(id).name("Paneer Tikka").price(price).active(active).available(available).restaurant(restaurant).build();
    }

    private CartItem cartItem(User customer, MenuItem menuItem, int quantity) {
        CartItem cartItem = new CartItem();
        cartItem.setMenuItem(menuItem);
        cartItem.setQuantity(quantity);
        cartItem.setUser(customer);
        cartItem.setId(UUID.randomUUID());
        return cartItem;
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

    private Restaurant restaurant(RestaurantStatus
            status) {
        return Restaurant.builder()
                .id(UUID.randomUUID())
                .name("Test Restaurant")
                .address("Test Street 1")
                .status(status)
                .build();
    }
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

    @Test
    void addItem_inactiveMenuItem_throws404() {

        User customer = user("customer@test.com", Role.CUSTOMER, UserStatus.ACTIVE);
        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);

        MenuItem item = menuItem(UUID.randomUUID(), restaurant, BigDecimal.ONE, false, true);
        CartItemRequest request = new CartItemRequest(item.getId(), 1);

        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.addItemToCart(request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getReason()).isEqualTo("Menu item not active");

        verify(menuItemRepository, times(1)).findById(item.getId());
    }
    @Test
    void addItem_quantityExceeded100_Throw400() {
        User customer = user("customer@test.com", Role.CUSTOMER, UserStatus.ACTIVE);
        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);

        MenuItem item = menuItem(UUID.randomUUID(), restaurant, BigDecimal.ONE, true, true);
        CartItem cartItem = cartItem(customer, item, 50);

        CartItemRequest request = new CartItemRequest(item.getId(), Integer.valueOf(80));

        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserAndMenuItem(customer, item)).thenReturn(Optional.of(cartItem));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.addItemToCart(request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).isEqualTo("Quantity exceeded, maximum allowed quantity is 100");

        verify(cartItemRepository, times(1)).findByUserAndMenuItem(customer, item);
    }

    @Test
    void addItem_differentRestaurant_Throw409() {
        User customer = user("customer@test.com", Role.CUSTOMER, UserStatus.ACTIVE);
        Restaurant restaurant1 = restaurant(RestaurantStatus.OPEN);
        Restaurant restaurant2 = restaurant(RestaurantStatus.OPEN);

        MenuItem existingItem = menuItem(UUID.randomUUID(), restaurant1, BigDecimal.ONE, true, true);
        MenuItem itemToAdd = menuItem(UUID.randomUUID(), restaurant2, BigDecimal.ONE, true, true);

        CartItem cartItem = cartItem(customer, existingItem, 50);
        CartItemRequest request = new CartItemRequest(itemToAdd.getId(), 1);

        when(menuItemRepository.findById(itemToAdd.getId())).thenReturn(Optional.of(itemToAdd));
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserOrderByCreatedAtAsc(customer)).thenReturn(List.of(cartItem));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.addItemToCart(request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Cart has item from different restaurant. clear it first");
    }

    //todo: net tests
    @Test
    void addItem_toSameRestaurant_addUp_priceAndQuantity() {
        User customer = user("customer@test.com", Role.CUSTOMER, UserStatus.ACTIVE);
        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);

        MenuItem item = menuItem(UUID.randomUUID(), restaurant, BigDecimal.valueOf(25), true, true);

        CartItem cartItem = cartItem(customer, item, 1);
        CartItemRequest request = new CartItemRequest(item.getId(), 2);

        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserAndMenuItem(customer, item)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.findByUserOrderByCreatedAtAsc(customer)).thenReturn(List.of(cartItem));
        CartResponse response = cartService.addItemToCart(request);

        assertThat(response.total()).isEqualTo(BigDecimal.valueOf(75));
        assertThat(response.items().size()).isEqualTo(1);
        verify(cartItemRepository).save(any());
    }

    @Test
    void updateItemQuantity_CustomerNotOwn_Cart_Throw404() {
        User customerOwnCart = user("customer@own.com", Role.CUSTOMER, UserStatus.ACTIVE);
        User customerNotOwnCart = user("customer@test.com", Role.CUSTOMER, UserStatus.ACTIVE);

        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);
        MenuItem item = menuItem(UUID.randomUUID(), restaurant, BigDecimal.valueOf(25), true, true);

        CartItem cartItem = cartItem(customerOwnCart, item, 50);
        CartQuantityUpdateRequest request = new CartQuantityUpdateRequest(5);

        when(cartItemRepository.findById(cartItem.getId())).thenReturn(Optional.of(cartItem));
        when(userRepository.findByEmail(customerNotOwnCart.getEmail())).thenReturn(Optional.of(customerNotOwnCart));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> cartService.updateItemQuantity(cartItem.getId(), request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getReason()).isEqualTo("Cart item not found");

        verify(cartItemRepository, never()).save(any());
    }
}
