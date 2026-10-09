package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.dto.CartItemRequest;
import dev.manu.food_delivery_app.dto.CartItemResponse;
import dev.manu.food_delivery_app.dto.CartQuantityUpdateRequest;
import dev.manu.food_delivery_app.dto.CartResponse;
import dev.manu.food_delivery_app.entity.CartItem;
import dev.manu.food_delivery_app.entity.MenuItem;
import dev.manu.food_delivery_app.entity.User;
import dev.manu.food_delivery_app.repository.CartItemRepository;
import dev.manu.food_delivery_app.repository.MenuItemRepository;
import dev.manu.food_delivery_app.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;

    //Helper: private functions for help

    private User getUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not authenticated"));
        return user;
    }

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
        return toCartResponse(cartItemRepository.findByUserOrderByCreatedAtAsc(getUser()));
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

        User user = getUser();

        Optional<CartItem> existing = cartItemRepository.findByUserAndMenuItem(user, menuItem);

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQuantity = item.getQuantity() + request.quantity();
            if (newQuantity > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Quantity exceeded, maximum allowed quantity is 100");
            }
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
            return toCartResponse(cartItemRepository.findByUserOrderByCreatedAtAsc(user));
        }

        for (CartItem ci : cartItemRepository.findByUserOrderByCreatedAtAsc(user)) {
            if (! ci.getMenuItem().isActive() || !ci.getMenuItem().isAvailable()) continue;

            if (!ci.getMenuItem().getRestaurant().getId().equals(menuItem.getRestaurant().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cart has item from different restaurant. clear it first");
            }
        }

        CartItem fresh = new CartItem();
        fresh.setUser(user);
        fresh.setMenuItem(menuItem);
        fresh.setQuantity(request.quantity());

        try {
            cartItemRepository.saveAndFlush(fresh);
        } catch (DataIntegrityViolationException e) {
            throw  new ResponseStatusException(HttpStatus.CONFLICT, "Cart item already exists");
        }

        return toCartResponse(cartItemRepository.findByUserOrderByCreatedAtAsc(user));
    }

    @Transactional
    public CartResponse updateItemQuantity(UUID itemId, CartQuantityUpdateRequest request) {
        User user = getUser();
        Optional<CartItem> cartItem = cartItemRepository.findById(itemId);

        if (cartItem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }

        if (!cartItem.get().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }

        cartItem.get().setQuantity(request.quantity());
        cartItemRepository.save(cartItem.get());
        return toCartResponse(cartItemRepository.findByUserOrderByCreatedAtAsc(user));
    }

    @Transactional
    public CartResponse removeItemFromCart(UUID itemId) {
        User user = getUser();
        Optional<CartItem> cartItem = cartItemRepository.findById(itemId);

        if (cartItem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }

        if (!cartItem.get().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }
        cartItemRepository.delete(cartItem.get());
        return toCartResponse(cartItemRepository.findByUserOrderByCreatedAtAsc(user));
    }

    @Transactional
    public void clearCart() {
        List<CartItem> cartItems = cartItemRepository.findByUserOrderByCreatedAtAsc(getUser());
        cartItemRepository.deleteAll(cartItems);
    }
}
