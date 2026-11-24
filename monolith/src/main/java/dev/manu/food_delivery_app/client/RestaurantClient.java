package dev.manu.food_delivery_app.client;

import dev.manu.food_delivery_app.dto.external.MenuItemSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RestaurantClient {
    private final RestClient restClient;

    public RestaurantClient(@Value("${app.service.restaurant.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public Map<UUID, MenuItemSnapshot> getMenu(UUID restaurantId) {
        List<MenuItemSnapshot> items = restClient.get()
                .uri("/api/v1/restaurants/{id}/menu", restaurantId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<MenuItemSnapshot>>() {});

        return (items == null ? List.<MenuItemSnapshot>of() : items)
                .stream()
                .collect(Collectors.toMap(MenuItemSnapshot::id, Function.identity()));
    }
}