package dev.manu.food_delivery_app.control;

import dev.manu.food_delivery_app.dto.admin.AdminOrderResponse;
import dev.manu.food_delivery_app.dto.admin.AdminRestaurantResponse;
import dev.manu.food_delivery_app.dto.order.PageResponse;
import dev.manu.food_delivery_app.service.AdminService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@AllArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AdminOrderResponse> getAllOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return adminService.getAllOrders(pageable);
    }

    @GetMapping("/restaurants")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminRestaurantResponse>  getAllRestaurants() {
        return adminService.getAllRestaurants();
    }
}
