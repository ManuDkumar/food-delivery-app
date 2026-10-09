package dev.manu.food_delivery_app.control;

import dev.manu.food_delivery_app.dto.CartItemRequest;
import dev.manu.food_delivery_app.dto.CartQuantityUpdateRequest;
import dev.manu.food_delivery_app.dto.CartResponse;
import dev.manu.food_delivery_app.service.CartService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@AllArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public CartResponse getCart() {
        return cartService.getCart();
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartResponse> addItemToCart(@Valid @RequestBody CartItemRequest cartItemRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItemToCart(cartItemRequest));
    }

    @PatchMapping("/items/{itemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CartResponse updateCart(
            @PathVariable UUID itemId, @Valid @RequestBody CartQuantityUpdateRequest request) {
        return cartService.updateItemQuantity(itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CartResponse deleteCartItem(@PathVariable UUID itemId) {
        return cartService.removeItemFromCart(itemId);
    }

    @DeleteMapping("/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart();
        return ResponseEntity.noContent().build();
    }

}
