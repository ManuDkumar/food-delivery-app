package dev.manu.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Component
public class JwtEdgeFilter implements GlobalFilter, Ordered {
    public static final List<PathPattern> PUBLIC_GETS = List.of(
            PathPatternParser.defaultInstance.parse("/api/v1/restaurants"),
            PathPatternParser.defaultInstance.parse("/api/v1/restaurants/{id}"),
            PathPatternParser.defaultInstance.parse("/api/v1/restaurants/{id}/menu")
    );

    private final JwtValidator jwtValidator;

    public JwtEdgeFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();

        if (isPublic(request.getMethod(), request.getPath().value())) {
            return chain.filter(exchange);
        }

        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange, "Authentication required");
        }

        try {
            jwtValidator.parse(header.substring(7));
        } catch (Exception e) {
            return unauthorized(exchange, "Invalid or expired token");
        }
        return chain.filter(exchange);
    }

    private boolean isPublic(HttpMethod method, String path) {

        if (path.equals("/actuator/health") || path.startsWith("/actuator/health/")) return true;

        if (method.equals(HttpMethod.POST)) {
            return path.equals("/api/v1/auth/login")
                    || path.equals("/api/v1/auth/customer/register")
                    || path.equals("/api/v1/auth/restaurant/register")
                    || path.equals("/api/v1/auth/admin/register");
        }

        if (method.equals(HttpMethod.GET)) {
            PathContainer pc = PathContainer.parsePath(path);
            return PUBLIC_GETS.stream().anyMatch(p -> p.matches(pc));
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """                                                                   
                     {"timeStamp":"%s","status":401,"error":"Unauthorized","message":"%s", "path":"%s"}
                     """.formatted(Instant.now(), message, exchange.getRequest().getPath().
                value()).trim();

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.
                UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}