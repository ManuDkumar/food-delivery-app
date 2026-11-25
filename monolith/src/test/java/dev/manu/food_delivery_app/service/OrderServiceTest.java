package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.client.RestaurantClient;
import dev.manu.food_delivery_app.dto.external.MenuItemSnapshot;
import dev.manu.food_delivery_app.dto.order.OrderResponse;
import dev.manu.food_delivery_app.dto.order.OrderStatusUpdateRequest;
import dev.manu.food_delivery_app.entity.*;
import dev.manu.food_delivery_app.enums.OrderStatus;
import dev.manu.food_delivery_app.enums.RestaurantStatus;
import dev.manu.food_delivery_app.enums.Role;
import dev.manu.food_delivery_app.repository.CartItemRepository;
import dev.manu.food_delivery_app.repository.OrderItemRepository;
import dev.manu.food_delivery_app.repository.OrderRepository;
import dev.manu.food_delivery_app.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    RestaurantClient restaurantClient;
    @Mock
    OrderItemRepository orderItemRepository;
    @InjectMocks
    private OrderService orderService;

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

    private User user(String email, Role role) {
        return User.builder()
                .email(email)
                .role(role)
                .createdAt(new Date())
                .id(UUID.randomUUID())
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

    private Restaurant restaurant(RestaurantStatus status) {
        return Restaurant.builder()
                .id(UUID.randomUUID())
                .name("Test Restaurant")
                .address("Test Street 1")
                .owner(user("Owner@Test.com", Role.RESTAURANT_OWNER))
                .status(status)
                .build();
    }

    private Order order(UUID customerId, Restaurant restaurant, OrderStatus orderStatus) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setCustomerName("Test Customer");
        order.setCustomerEmail("customer@test.com");
        order.setRestaurant(restaurant);
        order.setStatus(orderStatus);
        order.setId(UUID.randomUUID());
        order.setTotalPrice(BigDecimal.valueOf(100));
        order.setCreatedAt(new Date());
        return order;
    }

    private MenuItemSnapshot snapshot(MenuItem item, BigDecimal price, boolean available) {
        return new MenuItemSnapshot(item.getId(), item.getName(), price, true, available);
    }

    @Test
    void placeOrder_emptyCart_throws409() {
        UUID customerId = UUID.randomUUID();
        login(customerId);

        when(cartItemRepository.findCartWithMenuItems(customerId)).thenReturn(List.of());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.placeOrder());

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Cart is empty");
    }

    @Test
    void placeOrder_inactiveItem_throws409() {
        UUID customerId = UUID.randomUUID();
        login(customerId);

        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);
        MenuItem menuItem = menuItem(restaurant, BigDecimal.ZERO, true, true);
        CartItem cartItem = cartItem(customerId, menuItem, 10);

        when(cartItemRepository.findCartWithMenuItems(customerId)).thenReturn(List.of(cartItem));
        when(restaurantClient.getMenu(restaurant.getId())).thenReturn(
                Map.of(menuItem.getId(), snapshot(menuItem, BigDecimal.ZERO, false))
        );
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.placeOrder());

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Restaurant is not serving this item any more");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_clearsCart() {
        UUID customerId = UUID.randomUUID();
        login(customerId);

        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);
        MenuItem item = menuItem(restaurant, BigDecimal.ONE, true, true);
        CartItem cartItem = cartItem(customerId, item, 10);

        when(cartItemRepository.findCartWithMenuItems(customerId)).thenReturn(List.of(cartItem));
        when(restaurantClient.getMenu(restaurant.getId())).thenReturn(Map.of(
                item.getId(), snapshot(item, BigDecimal.ONE, true)
        ));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);

        orderService.placeOrder();

        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();

        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(saved.getCustomerName()).isEqualTo("Customer");
        assertThat(saved.getCustomerEmail()).isEqualTo("customer@test.com");
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(saved.getTotalPrice()).isEqualTo(BigDecimal.TEN);
        assertThat(saved.getOrderItems()).hasSize(1);

        verify(cartItemRepository, times(1)).deleteAll(List.of(cartItem));
    }

    @Test
    void getOrder_notOwner_throws404() {
        UUID ownerOfOrder = UUID.randomUUID();
        UUID somebodyElse = UUID.randomUUID();
        login(somebodyElse);

        Order order = order(ownerOfOrder, restaurant(RestaurantStatus.OPEN), OrderStatus.PLACED);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.getOrder(order.getId()));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getReason()).isEqualTo("Order not found");
    }

    @Test
    void cancelOrder_placed_cancels() {
        UUID customerId = UUID.randomUUID();
        login(customerId);

        Order order = order(customerId, restaurant(RestaurantStatus.OPEN), OrderStatus.PLACED);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0) );

        OrderResponse response = orderService.cancelOrder(order.getId());

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(any());
    }
    @Test
    void cancelOrder_delivered_throws409() {
        UUID customerId = UUID.randomUUID();
        login(customerId);

        Order order = order(customerId, restaurant(RestaurantStatus.OPEN), OrderStatus.DELIVERED);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.cancelOrder(order.getId()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Can't move from " + order.getStatus() + " to " + OrderStatus.CANCELLED);
    }
    @Test
    void updateOrderStatus_invalidTransaction_throws409() {
        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);
        login(restaurant.getOwner().getId());
        Order order = order(UUID.randomUUID(), restaurant, OrderStatus.PLACED);
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.OUT_FOR_DELIVERY);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.updateOrderStatus(order.getId(), request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Order status can't be updated from " + order.getStatus() + " to " + request.status());
    }
    @Test
    void updateOrderStatus_notOwner_throws404() {
        UUID customerId = UUID.randomUUID();
        login(customerId);

        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);
        Order order = order(customerId, restaurant, OrderStatus.PREPARING);
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.OUT_FOR_DELIVERY);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.updateOrderStatus(order.getId(), request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getReason()).isEqualTo("Order not found");
    }
}
