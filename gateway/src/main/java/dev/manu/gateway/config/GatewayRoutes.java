package dev.manu.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutes {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               @Value("${app.services.restaurant.base-url}") String restaurant,
                               @Value("${app.services.monolith.base-url}") String monolith) {
        return builder.routes()
                .route("restaurant-service", r -> r
                        .path("/api/v1/restaurants/**", "/api/v1/menu/**")
                        .uri(restaurant))
                .route("monolith", r -> r
                        .path("/api/v1/**")
                        .uri(monolith))
                .build();
    }
}