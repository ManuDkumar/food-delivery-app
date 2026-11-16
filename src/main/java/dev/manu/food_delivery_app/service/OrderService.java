package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.dto.order.OrderItemResponse;
import dev.manu.food_delivery_app.dto.order.OrderResponse;
import dev.manu.food_delivery_app.dto.order.OrderStatusUpdateRequest;
import dev.manu.food_delivery_app.dto.order.PageResponse;
import dev.manu.food_delivery_app.entity.*;
import dev.manu.food_delivery_app.enums.OrderStatus;
import dev.manu.food_delivery_app.repository.CartItemRepository;
import dev.manu.food_delivery_app.repository.OrderItemRepository;
import dev.manu.food_delivery_app.repository.OrderRepository;
import dev.manu.food_delivery_app.repository.UserRepository;
import dev.manu.food_delivery_app.security.AuthUserDetails;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;


    private User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof AuthUserDetails authUser) {
            return authUser.getUser();
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(email).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not authenticated")
        );
    }

    private OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getName(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()))
        );
    }



    @Transactional
    public OrderResponse placeOrder() {
        User currentUser = getUser();
        List<CartItem> cart = cartItemRepository.findCartWithMenuItems(currentUser);

        if (cart.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cart is empty");
        }

        for(CartItem cartItem : cart) {
            if(!cartItem.getMenuItem().isActive() || !cartItem.getMenuItem().isAvailable()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Restaurant is not serving this item any more");
            }
        }

        Restaurant restaurant = cart.get(0).getMenuItem().getRestaurant();
        BigDecimal totalPrice = BigDecimal.ZERO;


        Order order = new Order();
        order.setCustomer(currentUser);
        order.setRestaurant(restaurant);
        order.setStatus(OrderStatus.PLACED);

        for (CartItem cartItem : cart) {
            MenuItem menuItem = cartItem.getMenuItem();
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setName(menuItem.getName());
            orderItem.setUnitPrice(menuItem.getPrice());

            order.getOrderItems().add(orderItem);
            totalPrice = totalPrice.add(orderItem.getUnitPrice().
                    multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.setTotalPrice(totalPrice);
        Order savedOrder = orderRepository.save(order);

        cartItemRepository.deleteAll(cart);

        return toOrderResponse(savedOrder);

    }

    private OrderResponse toOrderResponse(Order savedOrder) {
        return toOrderResponse(savedOrder, savedOrder.getOrderItems());
    }

    private OrderResponse toOrderResponse(Order o, List<OrderItem> items) {
        return new OrderResponse(
                o.getId(),
                o.getRestaurant().getId(),
                o.getRestaurant().getName(),
                o.getStatus(),
                items.stream()
                        .map(this::toOrderItemResponse).toList(),
                o.getTotalPrice(),
                o.getCreatedAt()
        );
    }

    public PageResponse<OrderResponse> getMyOrders(Pageable pageable) {
        Page<Order> page = orderRepository.findMyOrders(getUser(), pageable);
        List<Order>  orders = page.getContent();

        Map<UUID, List<OrderItem>> itemsByOrder = orders.isEmpty() ? Map.of() :
                orderItemRepository.findItemsOfOrders(orders).stream()
                        .collect(Collectors.groupingBy(oi -> oi.getOrder().getId()));
        List<OrderResponse> content = orders.stream()
                .map(o -> toOrderResponse(o, itemsByOrder.getOrDefault(o.getId(), List.of())))
                .toList();
        return PageResponse.from(page, content);
    }

    public OrderResponse getOrder(UUID id) {
        Order order = fetchOrderById(id);
        return toOrderResponse(order);
    }

    private Order fetchOrderById(UUID id) {
        Order order = orderRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found")
        );
        if(!order.getCustomer().getId().equals(getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return order;
    }

    @Transactional
    public OrderResponse cancelOrder(UUID id) {
        Order order = fetchOrderById(id);

        if(!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Can't move from " + order.getStatus() + " to " + OrderStatus.CANCELLED);
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        return toOrderResponse(savedOrder);
    }

    public List<OrderResponse> getOwnerOrders() {
        return orderRepository.findByRestaurant_OwnerOrderByCreatedAtDesc(getUser())
                .stream().map(this::toOrderResponse).toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID id, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found")
        );

        if (!order.getRestaurant().getOwner().getId().equals(getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        if (!order.getStatus().canTransitionTo(request.status())){
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order status can't be updated from " + order.getStatus() + " to " + request.status());
        }
        order.setStatus(request.status());
        Order savedOrder = orderRepository.save(order);
        return toOrderResponse(savedOrder);
    }
}
