package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.dto.cart.CartItemRequest;
import dev.manu.food_delivery_app.dto.cart.CartItemResponse;
import dev.manu.food_delivery_app.dto.cart.CartQuantityUpdateRequest;
import dev.manu.food_delivery_app.dto.cart.CartResponse;
import dev.manu.food_delivery_app.entity.CartItem;
import dev.manu.food_delivery_app.entity.MenuItem;
import dev.manu.food_delivery_app.repository.CartItemRepository;
import dev.manu.food_delivery_app.repository.MenuItemRepository;
import dev.manu.food_delivery_app.security.CurrentUser;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final MenuItemRepository menuItemRepository;

    private CartItemResponse toCartItemResponse(CartItem cartItem) {
        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getMenuItem().getName(),
                cartItem.getMenuItem().getPrice(),
                cartItem.getQuantity(),
                cartItem.getMenuItem().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))
        );
    }

    private CartResponse toCartResponse(List<CartItem> cartItems) {

        List<CartItemResponse> activeItems = cartItems.stream()
                .filter(cartItem ->
                        cartItem.getMenuItem().isActive() && cartItem.getMenuItem().isAvailable()
                ).map(this::toCartItemResponse).toList();

        BigDecimal totalPrice = activeItems.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(activeItems, totalPrice);
    }

    //note: services here

    @Transactional(readOnly = true)
    public CartResponse getCart() {
        return toCartResponse(cartItemRepository.findCartWithMenuItems(CurrentUser.get().userId()));
    }

    @Transactional
    public CartResponse addItemToCart(CartItemRequest request) {
        MenuItem  menuItem = menuItemRepository.findById(request.menuItemId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found")
        );

        if (!menuItem.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not active");
        }

        if (!menuItem.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Menu item not available");
        }

        UUID userId = CurrentUser.get().userId();

        List<CartItem> cart = cartItemRepository.findCartWithMenuItems(userId);
        Optional<CartItem> existing = cart.stream()
                .filter(ci -> ci.getMenuItem().getId().equals(request.menuItemId())).findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQuantity = item.getQuantity() + request.quantity();
            if (newQuantity > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Quantity exceeded, maximum allowed quantity is 100");
            }
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
            return toCartResponse(cart);
        }

        for (CartItem ci : cart) {
            if (! ci.getMenuItem().isActive() || !ci.getMenuItem().isAvailable()) continue;

            if (!ci.getMenuItem().getRestaurant().getId().equals(menuItem.getRestaurant().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cart has item from different restaurant. clear it first");
            }
        }

        CartItem fresh = new CartItem();
        fresh.setUserId(userId);
        fresh.setMenuItem(menuItem);
        fresh.setQuantity(request.quantity());

        try {
            cartItemRepository.saveAndFlush(fresh);
        } catch (DataIntegrityViolationException e) {
            throw  new ResponseStatusException(HttpStatus.CONFLICT, "Cart item already exists");
        }
        cart.add(fresh);
        return toCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(UUID itemId, CartQuantityUpdateRequest request) {
        UUID userId = CurrentUser.get().userId();
        Optional<CartItem> cartItem = cartItemRepository.findById(itemId);

        if (cartItem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }

        if (!cartItem.get().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }

        cartItem.get().setQuantity(request.quantity());
        cartItemRepository.save(cartItem.get());
        return toCartResponse(cartItemRepository.findCartWithMenuItems(userId));
    }

    @Transactional
    public CartResponse removeItemFromCart(UUID itemId) {
        UUID userId = CurrentUser.get().userId();
        Optional<CartItem> cartItem = cartItemRepository.findById(itemId);

        if (cartItem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }

        if (!cartItem.get().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }
        cartItemRepository.delete(cartItem.get());
        return toCartResponse(cartItemRepository.findCartWithMenuItems(userId));
    }

    @Transactional
    public void clearCart() {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtAsc(CurrentUser.get().userId());
        cartItemRepository.deleteAll(cartItems);
    }
}
