package dev.manu.restaurant_service.control;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "service", "restaurant-service",
                "status", "alive",
                "at", LocalTime.now().toString()
        );
    }
}