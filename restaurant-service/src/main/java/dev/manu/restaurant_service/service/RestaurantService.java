package dev.manu.restaurant_service.service;

import dev.manu.restaurant_service.dto.restaurant.PublicRestaurantResponse;
import dev.manu.restaurant_service.dto.restaurant.RestaurantCreateRequest;
import dev.manu.restaurant_service.dto.restaurant.RestaurantUpdateRequest;
import dev.manu.restaurant_service.entity.Restaurant;
import dev.manu.restaurant_service.enums.RestaurantStatus;
import dev.manu.restaurant_service.repository.RestaurantRepository;
import dev.manu.restaurant_service.security.CurrentUser;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    @Cacheable("restaurants")
    @Transactional(readOnly = true)
    public List<PublicRestaurantResponse> listAll() {

        return restaurantRepository.findByStatusNot(RestaurantStatus.REMOVED).stream()
                .map(
                    r -> getPublicRestaurantResponse(r)
                ).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicRestaurantResponse> getMine() {

        CurrentUser user = CurrentUser.get();
        return restaurantRepository.findByOwnerId(user.userId()).stream()
                .map(
                        r -> getPublicRestaurantResponse(r)
                ).toList();
    }


    @Transactional(readOnly = true)
    public PublicRestaurantResponse getRestaurant(UUID id) {

        Restaurant restaurant = getRestaurantOrThrow(id);
        return getPublicRestaurantResponse(restaurant);
    }
    @CacheEvict(value = "restaurants", allEntries = true)
    @Transactional
    public PublicRestaurantResponse createRestaurant(RestaurantCreateRequest restaurantCreateRequest) {
        CurrentUser user = CurrentUser.get();

        Restaurant restaurant = Restaurant.builder()
                .name(restaurantCreateRequest.name())
                .ownerId(user.userId())
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

    @CacheEvict(value = "restaurants", allEntries = true)
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

    @CacheEvict(value = "restaurants", allEntries = true)
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
        CurrentUser user = CurrentUser.get();

        if (! user.owns(restaurant.getOwnerId()) && !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to perform this action.");
        }
    }
}
