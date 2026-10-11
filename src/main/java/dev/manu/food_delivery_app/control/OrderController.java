package dev.manu.food_delivery_app.control;

import dev.manu.food_delivery_app.dto.OrderResponse;
import dev.manu.food_delivery_app.dto.OrderStatusUpdateRequest;
import dev.manu.food_delivery_app.service.OrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@AllArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> placeOrder() {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder());
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<OrderResponse> getCustomerOrder() {
        return orderService.getMyOrders();
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse getCustomerOrder(@PathVariable UUID orderId) {
        return orderService.getOrder(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse cancelOrder(@PathVariable UUID orderId) {
        return orderService.cancelOrder(orderId);
    }

    @GetMapping("/restaurant/mine")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public List<OrderResponse> getOwnerOrder() {
        return orderService.getOwnerOrders();
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public OrderResponse updateOrderStatus(
            @PathVariable UUID orderId, @Valid @RequestBody OrderStatusUpdateRequest statusUpdateRequest) {
        return orderService.updateOrderStatus(orderId, statusUpdateRequest);
    }
}
