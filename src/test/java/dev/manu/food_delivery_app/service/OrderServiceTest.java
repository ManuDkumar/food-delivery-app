package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.dto.order.OrderResponse;
import dev.manu.food_delivery_app.dto.order.OrderStatusUpdateRequest;
import dev.manu.food_delivery_app.entity.*;
import dev.manu.food_delivery_app.enums.OrderStatus;
import dev.manu.food_delivery_app.enums.RestaurantStatus;
import dev.manu.food_delivery_app.enums.Role;
import dev.manu.food_delivery_app.repository.CartItemRepository;
import dev.manu.food_delivery_app.repository.OrderRepository;
import dev.manu.food_delivery_app.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @InjectMocks
    private OrderService orderService;

    private User user(String email) {
        return User.builder().email(email).role(Role.CUSTOMER).createdAt(new Date()).id(UUID.randomUUID()).build();
    }
    private MenuItem menuItem(
            UUID id, Restaurant restaurant, BigDecimal price, boolean active, boolean available) {
        return MenuItem.builder().id(id).name("Paneer Tikka").price(price).active(active).available(available).restaurant(restaurant).build();
    }

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer@test.com", null, List.of())
        );
    }
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }


    private CartItem cartItem(User customer, MenuItem menuItem, int quantity) {
        CartItem cartItem = new CartItem();
        cartItem.setMenuItem(menuItem);
        cartItem.setQuantity(quantity);
        cartItem.setUser(customer);
        cartItem.setId(UUID.randomUUID());
        return cartItem;
    }

    private Restaurant restaurant(RestaurantStatus
                                          status) {
        return Restaurant.builder()
                .id(UUID.randomUUID())
                .name("Test Restaurant")
                .address("Test Street 1")
                .owner(user("Owner@Test.com"))
                .status(status)
                .build();
    }

    private Order order(User customer, Restaurant restaurant, OrderStatus orderStatus) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setStatus(orderStatus);
        order.setId(UUID.randomUUID());
        order.setTotalPrice(BigDecimal.valueOf(100));
        order.setCreatedAt(new Date());
        return order;
    }

    @Test
    void placeOrder_emptyCart_throws409() {
        User customer =  user("customer@test.com");

        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserOrderByCreatedAtAsc(customer)).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.placeOrder());

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Cart is empty");
    }

    @Test
    void placeOrder_inactiveItem_throws409() {
        User customer =  user("customer@test.com");
        MenuItem menuItem = menuItem(UUID.randomUUID(), null, BigDecimal.ZERO, false, true);
        CartItem cartItem = cartItem(customer, menuItem, 10);

        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserOrderByCreatedAtAsc(customer)).thenReturn(List.of(cartItem));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.placeOrder());

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Restaurant is not serving this item any more");
    }

    @Test
    void placeOrder_clearsCart() {
        User customer = user("customer@test.com");
        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);
        MenuItem menuItem = menuItem(UUID.randomUUID(), restaurant, BigDecimal.ONE, true, true);
        CartItem cartItem = cartItem(customer, menuItem, 10);

        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserOrderByCreatedAtAsc(customer)).thenReturn(List.of(cartItem));

        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);

        orderService.placeOrder();

        verify(orderRepository).save(captor.capture());

        assertThat(captor.getValue().getTotalPrice()).isEqualTo(BigDecimal.TEN);
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(captor.getValue().getOrderItems().size()).isEqualTo(1);

        verify(cartItemRepository, times(1)).deleteAll(List.of(cartItem));
    }

    @Test
    void getOrder_notOwner_throws404() {
        User customerOwnsCart = user("customer@own.com");
        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);
        Order order = order(customerOwnsCart, restaurant, OrderStatus.PLACED);

        User customerNotOwn = user("customer@test.com");

        when(userRepository.findByEmail(customerNotOwn.getEmail())).thenReturn(Optional.of(customerNotOwn));
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.getOrder(order.getId()));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getReason()).isEqualTo("Order not found");
    }

    @Test
    void cancelOrder_placed_cancels() {
        User customer = user("customer@test.com");
        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);
        Order order = order(customer, restaurant, OrderStatus.PLACED);

        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0) );

        OrderResponse response = orderService.cancelOrder(order.getId());

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(any());
    }
    @Test
    void cancelOrder_delivered_throws409() {
        User customer = user("customer@test.com");
        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);
        Order order = order(customer, restaurant, OrderStatus.DELIVERED);

        when(userRepository.findByEmail(any())).thenReturn(Optional.of(customer));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.cancelOrder(order.getId()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Can't move from " + order.getStatus() + " to " + OrderStatus.CANCELLED);
    }
    @Test
    void updateOrderStatus_invalidTransaction_throws409() {
        User customer = user("customer@test.com");
        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);
        Order order = order(customer, restaurant, OrderStatus.PLACED);
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.OUT_FOR_DELIVERY);

        when(userRepository.findByEmail(any())).thenReturn(Optional.of(order.getRestaurant().getOwner()));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.updateOrderStatus(order.getId(), request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Order status can't be updated from " + order.getStatus() + " to " + request.status());
    }
    @Test
    void updateOrderStatus_notOwner_throws404() {
        User customer = user("customer@test.com");
        Restaurant restaurant = restaurant(RestaurantStatus.OPEN);
        Order order = order(customer, restaurant, OrderStatus.PREPARING);
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.OUT_FOR_DELIVERY);

        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.updateOrderStatus(order.getId(), request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getReason()).isEqualTo("Order not found");
    }
}
