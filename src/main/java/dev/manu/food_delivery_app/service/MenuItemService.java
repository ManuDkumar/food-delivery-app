package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.dto.MenuItemRequest;
import dev.manu.food_delivery_app.dto.MenuItemResponse;
import dev.manu.food_delivery_app.dto.MenuItemUpdateRequest;
import dev.manu.food_delivery_app.entity.MenuItem;
import dev.manu.food_delivery_app.entity.Restaurant;
import dev.manu.food_delivery_app.entity.User;
import dev.manu.food_delivery_app.enums.RestaurantStatus;
import dev.manu.food_delivery_app.enums.Role;
import dev.manu.food_delivery_app.repository.MenuItemRepository;
import dev.manu.food_delivery_app.repository.RestaurantRepository;
import dev.manu.food_delivery_app.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    // helper functions
    private MenuItem getMenuItemOrThrow(UUID id){
        return menuItemRepository.findById(id)
                .filter(MenuItem::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MenuItem not found"));
    }

    private MenuItemResponse convertToMenuItemResponse(MenuItem menuItem){
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getPrice(),
                menuItem.isVeg(),
                menuItem.isAvailable(),
                menuItem.getCreatedAt(),
                menuItem.getUpdatedAt()
        );
    }

    private User getUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not authenticated"));
        return user;
    }


    private Restaurant getRestaurantOrThrow(UUID id) {
        Restaurant restaurant = restaurantRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,  "Restaurant not found")
        );
        if(restaurant.getStatus().equals(RestaurantStatus.REMOVED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Restaurant is not available at this address.");
        }
        return restaurant;
    }

    private void assertOwnerOrAdmin(Restaurant restaurant) {
        User user = getUser();
        boolean allowed = restaurant.getOwner().getId().equals(user.getId()) ||
                user.getRole() == Role.ADMIN;

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to perform this action.");
        }
    }

    // services for menu items

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenuItems(UUID restaurantId) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);

        return menuItemRepository.findByRestaurantAndActiveTrueOrderByCreatedAtDesc(restaurant)
                .stream().map(this::convertToMenuItemResponse).toList();
    }

    @Transactional
    public MenuItemResponse createMenuItem(UUID restaurantId, MenuItemRequest menuItemRequest){
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);

        assertOwnerOrAdmin(restaurant);

        MenuItem menuItem = MenuItem.builder()
                .name(menuItemRequest.name())
                .description(menuItemRequest.description())
                .price(menuItemRequest.price())
                .veg(menuItemRequest.veg())
                .restaurant(restaurant)
                .build();
        MenuItem savedMenuItem = menuItemRepository.save(menuItem);
        return convertToMenuItemResponse(savedMenuItem);
    }

    @Transactional
    public MenuItemResponse updateMenuItem(UUID itemId, MenuItemUpdateRequest request){

        MenuItem menuItem = getMenuItemOrThrow(itemId);
        assertOwnerOrAdmin(menuItem.getRestaurant());

        if (request.name() != null) {
            menuItem.setName(request.name());
        }
        if (request.description() != null) {
            menuItem.setDescription(request.description());
        }
        if (request.price() != null) {
            menuItem.setPrice(request.price());
        }
        if (request.veg() != null) {
            menuItem.setVeg(request.veg());
        }

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);
        return convertToMenuItemResponse(savedMenuItem);
    }

    @Transactional
    public MenuItemResponse toggleMenuItemAvailability(UUID menuItemId, boolean available) {
        MenuItem menuItem = getMenuItemOrThrow(menuItemId);
        assertOwnerOrAdmin(menuItem.getRestaurant());
        menuItem.setAvailable(available);
        MenuItem savedMenuItem = menuItemRepository.save(menuItem);
        return convertToMenuItemResponse(savedMenuItem);
    }

    @Transactional
    public void deleteMenuItem(UUID menuItemId) {
        MenuItem menuItem = getMenuItemOrThrow(menuItemId);

        assertOwnerOrAdmin(menuItem.getRestaurant());

        menuItem.setActive(false);
        menuItemRepository.save(menuItem);
    }
}
