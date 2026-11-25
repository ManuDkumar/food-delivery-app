package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.dto.cart.CartItemRequest;
import dev.manu.food_delivery_app.dto.cart.CartQuantityUpdateRequest;
import dev.manu.food_delivery_app.dto.cart.CartResponse;
import dev.manu.food_delivery_app.entity.CartItem;
import dev.manu.food_delivery_app.entity.MenuItem;
import dev.manu.food_delivery_app.entity.Restaurant;
import dev.manu.food_delivery_app.enums.RestaurantStatus;
import dev.manu.food_delivery_app.repository.CartItemRepository;
import dev.manu.food_delivery_app.repository.MenuItemRepository;
import dev.manu.food_delivery_app.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
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
    private MenuItemRepository menuItemRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @InjectMocks
    private CartService cartService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }
    private CurrentUser login(UUID userId) {
        CurrentUser principal = new CurrentUser(userId, "customer@test.com", "Customer", "CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        return principal;
    }

    private Restaurant restaurant() {
        return Restaurant.builder()
                .id(UUID.randomUUID())
                .name("Test Restaurant")
                .address("Test Street 1")
                .status(RestaurantStatus.OPEN)
                .build();
    }
    private MenuItem menuItem(
            Restaurant restaurant, BigDecimal price, boolean active, boolean available) {
        return MenuItem.builder()
                .id(UUID.randomUUID())
                .name("Paneer Tikka")
                .price(price)
                .active(active)
                .available(available)
                .restaurant(restaurant)
                .build();
    }

    private CartItem cartItem(UUID userId, MenuItem menuItem, int quantity) {
        CartItem cartItem = new CartItem();
        cartItem.setMenuItem(menuItem);
        cartItem.setQuantity(quantity);
        cartItem.setUserId(userId);
        cartItem.setId(UUID.randomUUID());
        return cartItem;
    }

    @Test
    void addItem_inactiveMenuItem_throws404() {
        MenuItem item = menuItem(restaurant(), BigDecimal.ONE, false, true);
        CartItemRequest request = new CartItemRequest(item.getId(), 1);

        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.addItemToCart(request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getReason()).isEqualTo("Menu item not active");

        verify(menuItemRepository, never()).saveAndFlush(any());
    }
    @Test
    void addItem_quantityExceeded100_Throw400() {
        UUID customerId = UUID.randomUUID();
        login(customerId);

        MenuItem item = menuItem(restaurant(), BigDecimal.ONE, true, true);
        CartItem cartItem = cartItem(customerId, item, 50);

        CartItemRequest request = new CartItemRequest(item.getId(), 80);

        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(cartItemRepository.findCartWithMenuItems(customerId)).thenReturn(List.of(cartItem));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.addItemToCart(request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).isEqualTo("Quantity exceeded, maximum allowed quantity is 100");
    }

    @Test
    void addItem_differentRestaurant_Throw409() {
        UUID customerId = UUID.randomUUID();
        login(customerId);

        MenuItem existingItem = menuItem(restaurant(), BigDecimal.ONE, true, true);
        MenuItem itemToAdd = menuItem(restaurant(), BigDecimal.ONE, true, true);

        CartItem existing = cartItem(customerId, existingItem, 2);
        CartItemRequest request = new CartItemRequest(itemToAdd.getId(), 1);

        when(menuItemRepository.findById(itemToAdd.getId())).thenReturn(Optional.of(itemToAdd));
        when(cartItemRepository.findCartWithMenuItems(customerId)).thenReturn(List.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.addItemToCart(request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Cart has item from different restaurant. clear it first");
    }

    @Test
    void addItem_toSameRestaurant_addUp_priceAndQuantity() {
        UUID customerId = UUID.randomUUID();
        login(customerId);

        MenuItem item = menuItem(restaurant(), BigDecimal.valueOf(25), true, true);
        CartItem existing = cartItem(customerId, item, 1);
        CartItemRequest request = new CartItemRequest(item.getId(), 2);

        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(cartItemRepository.findCartWithMenuItems(customerId)).thenReturn(List.of(existing));
        CartResponse response = cartService.addItemToCart(request);

        assertThat(response.total()).isEqualTo(BigDecimal.valueOf(75));
        assertThat(response.items().size()).isEqualTo(1);
        assertThat(existing.getQuantity()).isEqualTo(3);
        verify(cartItemRepository).save(existing);
    }

    @Test
    void updateItemQuantity_CustomerNotOwn_Cart_Throw404() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        login(otherId);

        MenuItem item = menuItem(restaurant(), BigDecimal.valueOf(25), true, true);

        CartItem cartItem = cartItem(ownerId, item, 5);
        CartQuantityUpdateRequest request = new CartQuantityUpdateRequest(5);

        when(cartItemRepository.findById(cartItem.getId())).thenReturn(Optional.of(cartItem));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                cartService.updateItemQuantity(cartItem.getId(), new  CartQuantityUpdateRequest(10)));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getReason()).isEqualTo("Cart item not found");

        verify(cartItemRepository, never()).save(any());
    }
}
