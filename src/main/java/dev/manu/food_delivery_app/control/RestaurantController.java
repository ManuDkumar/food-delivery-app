package dev.manu.food_delivery_app.control;

import dev.manu.food_delivery_app.dto.PublicRestaurantResponse;
import dev.manu.food_delivery_app.dto.RestaurantCreateRequest;
import dev.manu.food_delivery_app.dto.RestaurantUpdateRequest;
import dev.manu.food_delivery_app.enums.RestaurantStatus;
import dev.manu.food_delivery_app.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/restaurants")
@AllArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<PublicRestaurantResponse> createRestaurant(
            @Valid @RequestBody RestaurantCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.createRestaurant(request));
    }

    @GetMapping
    public List<PublicRestaurantResponse>  listAllRestaurants() {
        return restaurantService.listAll();
    }

    @GetMapping("/owner/mine")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public List<PublicRestaurantResponse> getMyRestaurants() {
        return restaurantService.getMine();
    }

    @GetMapping("/{id}")
    public PublicRestaurantResponse getRestaurantById(@PathVariable UUID id) {
        return restaurantService.getRestaurant(id);
    }

    @PatchMapping("/owner/{restaurantId}/status")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    public PublicRestaurantResponse updateRestaurantStatus(
            @PathVariable UUID restaurantId, @RequestBody RestaurantStatus status) {
        return  restaurantService.updateRestaurantStatus(restaurantId, status);
    }

    @PatchMapping("/owner/{id}/update")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    public PublicRestaurantResponse updateRestaurant(
            @PathVariable UUID id, @Valid @RequestBody RestaurantUpdateRequest request) {
        return restaurantService.updateRestaurant(id, request);
    }

    @DeleteMapping("/owner/{id}/delete")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','ADMIN')")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable UUID id) {
        return restaurantService.deleteRestaurant(id);
    }
}
