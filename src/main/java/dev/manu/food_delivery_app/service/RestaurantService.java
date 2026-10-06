package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.dto.PublicRestaurantResponse;
import dev.manu.food_delivery_app.dto.RestaurantCreateRequest;
import dev.manu.food_delivery_app.dto.RestaurantUpdateRequest;
import dev.manu.food_delivery_app.entity.Restaurant;
import dev.manu.food_delivery_app.entity.User;
import dev.manu.food_delivery_app.enums.RestaurantStatus;
import dev.manu.food_delivery_app.enums.Role;
import dev.manu.food_delivery_app.repository.RestaurantRepository;
import dev.manu.food_delivery_app.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public List<PublicRestaurantResponse> listAll() {

        return restaurantRepository.findByStatusNot(RestaurantStatus.REMOVED).stream()
                .map(
                    r -> getPublicRestaurantResponse(r)
                ).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicRestaurantResponse> getMine() {

        User user = getUser();
        return restaurantRepository.findByOwner(user).stream()
                .map(
                        r -> getPublicRestaurantResponse(r)
                ).toList();
    }


    @Transactional(readOnly = true)
    public PublicRestaurantResponse getRestaurant(UUID id) {

        Restaurant restaurant = getRestaurantOrThrow(id);
        return getPublicRestaurantResponse(restaurant);
    }

    @Transactional
    public PublicRestaurantResponse createRestaurant(RestaurantCreateRequest restaurantCreateRequest) {
        User user = getUser();

        Restaurant restaurant = Restaurant.builder()
                .name(restaurantCreateRequest.name())
                .owner(user)
                .phone(restaurantCreateRequest.phone())
                .address(restaurantCreateRequest.address())
                .description(restaurantCreateRequest.description())
                .status(RestaurantStatus.CLOSED) // explicitly
                .build();

        try {
            restaurantRepository.saveAndFlush(restaurant);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Restaurant already exists at this address.");
        }
        return getPublicRestaurantResponse(restaurant);
    }

    @Transactional
    public PublicRestaurantResponse updateRestaurant(
            UUID id, RestaurantUpdateRequest restaurantUpdateRequest) {
        Restaurant restaurant = getRestaurantOrThrow(id);
        assertOwnerOrAdmin(restaurant);
        if (restaurantUpdateRequest.name() != null) {
            restaurant.setName(restaurantUpdateRequest.name());
        }
        if (restaurantUpdateRequest.phone() != null) {
            restaurant.setPhone(restaurantUpdateRequest.phone());
        }
        if (restaurantUpdateRequest.address() != null) {
            restaurant.setAddress(restaurantUpdateRequest.address());
        }
        if (restaurantUpdateRequest.description() != null) {
            restaurant.setDescription(restaurantUpdateRequest.description());
        }
        Restaurant updatedRestaurant = null;
        try {
            updatedRestaurant = restaurantRepository.saveAndFlush(restaurant);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Conflict occurred during update restaurant.");
        }
        return getPublicRestaurantResponse(updatedRestaurant);
    }

    @Transactional
    public PublicRestaurantResponse updateRestaurantStatus(
            UUID id, RestaurantStatus restaurantStatus) {
        Restaurant restaurant = getRestaurantOrThrow(id);


        if (restaurantStatus == RestaurantStatus.REMOVED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "DELETE Operation not allowed in this endpoint");
        }

        assertOwnerOrAdmin(restaurant);
        restaurant.setStatus(restaurantStatus);
        Restaurant updatedRestaurant = null;
        try {
            updatedRestaurant = restaurantRepository.save(restaurant);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Conflict occurred during update status of restaurant.");
        }
        return getPublicRestaurantResponse(updatedRestaurant);
    }


    @Transactional
    public ResponseEntity<Void> deleteRestaurant(UUID id) {
        Restaurant restaurant = getRestaurantOrThrow(id);
        assertOwnerOrAdmin(restaurant);

        restaurant.setStatus(RestaurantStatus.REMOVED);
        restaurant.setPhone(restaurant.getId().toString().replace("-", ""));
        restaurant.setAddress(restaurant.getId().toString().replace("-", ""));
        restaurant.setDescription(restaurant.getId().toString().replace("-", ""));
        restaurant.setName(restaurant.getId().toString().replace("-", ""));
        restaurantRepository.save(restaurant);
        return ResponseEntity.noContent().build();
    }

    private User getUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not authenticated"));
        return user;
    }

    private PublicRestaurantResponse getPublicRestaurantResponse(Restaurant restaurant) {
        return new PublicRestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.getStatus(),
                restaurant.getCreatedAt(),
                restaurant.getUpdatedAt()
        );
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
}
