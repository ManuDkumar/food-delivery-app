package dev.manu.food_delivery_app.control;

import dev.manu.food_delivery_app.dto.MenuItemRequest;
import dev.manu.food_delivery_app.dto.MenuItemResponse;
import dev.manu.food_delivery_app.dto.MenuItemUpdateRequest;
import dev.manu.food_delivery_app.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class MenuItemController {
    private final MenuItemService menuItemService;

    @GetMapping("/restaurants/{restaurantId}/menu")
    public List<MenuItemResponse> getMenu(@PathVariable UUID restaurantId) {
        return menuItemService.getMenuItems(restaurantId);
    }

    @PostMapping("/restaurants/{restaurantId}/menu")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseEntity<MenuItemResponse> createMenuItem(
            @Valid @RequestBody MenuItemRequest menuItemRequest, @PathVariable UUID restaurantId) {
        return  ResponseEntity.status(HttpStatus.CREATED)
                .body(menuItemService.createMenuItem(restaurantId, menuItemRequest));
    }
    @PatchMapping("/menu/{itemId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public MenuItemResponse updateMenuItem(
            @Valid @RequestBody MenuItemUpdateRequest request, @PathVariable UUID itemId) {
        return menuItemService.updateMenuItem(itemId, request);
    }

    @PatchMapping("/menu/{itemId}/availability")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public MenuItemResponse toggleMenuItem(@PathVariable UUID itemId, @RequestBody boolean available ) {
        return menuItemService.toggleMenuItemAvailability(itemId, available);
    }

    @DeleteMapping("/menu/{itemId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable UUID itemId) {
        menuItemService.deleteMenuItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
