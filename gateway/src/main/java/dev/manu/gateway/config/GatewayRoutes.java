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
                               @Value("${app.services.monolith.base-url}") String monolith,
                               @Value("${app.services.user.base-url}") String user) {
        return builder.routes()
                .route("user-service", r -> r
                        .path("/api/v1/auth/**", "/api/v1/user/**")
                        .uri(user))
                .route("user-service-admin", r -> r
                        .path("/api/v1/admin/users", "/api/v1/admin/users/**")
                        .uri(user))
                .route("restaurant-service", r -> r
                        .path("/api/v1/restaurants/**", "/api/v1/menu/**")
                        .uri(restaurant))
                .route("monolith", r -> r
                        .path("/api/v1/**")
                        .uri(monolith))
                .build();
    }
}