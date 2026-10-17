package dev.manu.food_delivery_app.integration;

import com.jayway.jsonpath.JsonPath;
import dev.manu.food_delivery_app.dto.auth.LoginRequest;
import dev.manu.food_delivery_app.dto.cart.CartItemRequest;
import dev.manu.food_delivery_app.dto.order.OrderStatusUpdateRequest;
import dev.manu.food_delivery_app.dto.user.RegisterRequest;
import dev.manu.food_delivery_app.entity.MenuItem;
import dev.manu.food_delivery_app.entity.Restaurant;
import dev.manu.food_delivery_app.entity.User;
import dev.manu.food_delivery_app.enums.OrderStatus;
import dev.manu.food_delivery_app.enums.RestaurantStatus;
import dev.manu.food_delivery_app.enums.Role;
import dev.manu.food_delivery_app.enums.UserStatus;
import dev.manu.food_delivery_app.repository.MenuItemRepository;
import dev.manu.food_delivery_app.repository.RestaurantRepository;
import dev.manu.food_delivery_app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
public class FlowIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RestaurantRepository restaurantRepository;
    @Autowired
    private MenuItemRepository menuItemRepository;

    private RegisterRequest registerRequest(String email, String phone) {
        return new RegisterRequest("Hero", email, "Abcd@1234", phone);
    }

    private LoginRequest loginRequest(String email, String password) {
        return new LoginRequest(email, password);
    }

    @Test
    void register_duplicateEmail_return409WithBody() throws Exception {
        String json = om.writeValueAsString(registerRequest("hero@test.com", "9876543210"));

        mvc.perform(post("/api/v1/auth/customer/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }
    @Test
    void loginWith_wrongPassword_return401() throws Exception {
        String json = om.writeValueAsString(registerRequest("hero@test.com", "9876543210"));

        mvc.perform(post("/api/v1/auth/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        String loginJson = om.writeValueAsString(loginRequest("hero@test.com", "Wrong@4321"));

        mvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
    @Test
    void loginWith_validCredentials_return200() throws Exception {
        String json = om.writeValueAsString(registerRequest("hero@test.com", "9876543210"));

        mvc.perform(post("/api/v1/auth/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        String loginJson = om.writeValueAsString(loginRequest("hero@test.com", "Abcd@1234"));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("hero@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void registerLogin_getProfile_return200() throws Exception {
        String json = om.writeValueAsString(registerRequest("hero@test.com", "9876543210"));

        mvc.perform(post("/api/v1/auth/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        String loginJson = om.writeValueAsString(loginRequest("hero@test.com", "Abcd@1234"));

        String token = loginAnd_getToken(loginJson);

        mvc.perform(get("/api/v1/user/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("hero@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.phone").value("9876543210"));
    }

    private String loginAnd_getToken(String loginJson) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private Restaurant seedRestaurant(RestaurantStatus status, User owner, String name) {

        return restaurantRepository.save(Restaurant.builder()
                .name(name)
                .owner(owner)
                .address("Seed street 1")
                .phone("7912234560")
                .status(status)
                .build());
    }

    private User seedOwner() {
        return userRepository.save(User.builder()
                .name("Owner")
                .email("owner@test.com")
                .password(passwordEncoder.encode("Abcd@1234"))
                .phone("9712234560")
                .role(Role.RESTAURANT_OWNER).build());
    }

    private MenuItem seedMenuItem(Restaurant r) {
        return menuItemRepository.save(MenuItem.builder()
                        .name("Paneer Tikka")
                        .price(BigDecimal.valueOf(120))
                        .restaurant(r)
                .build());
    }
    @Test
    void addToCart_placeOrder_cancelAvailable_return200() throws Exception {
        User owner = seedOwner();
        Restaurant restaurant = seedRestaurant(RestaurantStatus.OPEN, owner, "Seed Kitchen");
        MenuItem menuItem = seedMenuItem(restaurant);

        String json = om.writeValueAsString(registerRequest("hero@test.com", "9876543210"));

        mvc.perform(post("/api/v1/auth/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        String loginJson = om.writeValueAsString(loginRequest("hero@test.com", "Abcd@1234"));

        String token = loginAnd_getToken(loginJson);

        mvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new CartItemRequest(menuItem.getId(), 2))
                        ).header("Authorization", "Bearer " + token)
        ).andExpect(status().isCreated())
         .andExpect(jsonPath("$.items.length()").value(1))
         .andExpect(jsonPath("$.items[0].quantity").value(2));

        MvcResult order = mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
        ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andReturn();

        String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
        ).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void publicBrowse_excludesRemoved() throws Exception {
        User owner = seedOwner();
        Restaurant open = seedRestaurant(RestaurantStatus.OPEN, owner, "Open Kitchen");
        Restaurant removed = seedRestaurant(RestaurantStatus.REMOVED, owner, "Removed Kitchen");

        MvcResult response = mvc.perform(get("/api/v1/restaurants")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        List<String> names = JsonPath.read(response.getResponse().getContentAsString(), "$[*].restaurantName");

        assertThat(names.contains(open.getName())).isTrue();
        assertThat(names.contains(removed.getName())).isFalse();
    }
    @Test
    void ownerOrderUpdate_transitionAndRole() throws Exception {
        User owner = seedOwner();
        Restaurant restaurant = seedRestaurant(RestaurantStatus.OPEN, owner, "Open Kitchen");
        MenuItem menuItem = seedMenuItem(restaurant);

        User customer = User.builder()
                .name("customer")
                .role(Role.CUSTOMER)
                .password(passwordEncoder.encode("Abcd@1234"))
                .phone("8756746359")
                .email("customer@test.com").build();
        userRepository.save(customer);

        String customerLoginJson = om.writeValueAsString(loginRequest(customer.getEmail(), "Abcd@1234"));
        String customerToken = loginAnd_getToken(customerLoginJson);

        mvc.perform(post("/api/v1/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(new CartItemRequest(menuItem.getId(), 2)))
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult order = mvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken)
                ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andReturn();

        String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.id");

        String ownerLoginJson = om.writeValueAsString(loginRequest(owner.getEmail(), "Abcd@1234"));
        String ownerToken = loginAnd_getToken(ownerLoginJson);

        mvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(new OrderStatusUpdateRequest(OrderStatus.OUT_FOR_DELIVERY)))
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order status can't be updated from "
                        + OrderStatus.PLACED + " to " + OrderStatus.OUT_FOR_DELIVERY));

        mvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new OrderStatusUpdateRequest(OrderStatus.CONFIRMED)))
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));

        mvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new OrderStatusUpdateRequest(OrderStatus.CONFIRMED)))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
    @Test
    void adminBlocksOwner_restaurantCloses() throws Exception {
        User owner = seedOwner();
        Restaurant restaurant = seedRestaurant(RestaurantStatus.OPEN, owner, "Open Kitchen");

        User admin = User.builder()
                .name("admin")
                .password(passwordEncoder.encode("Admin@123"))
                .email("admin@test.com")
                .phone("8756746359")
                .role(Role.ADMIN).build();
        userRepository.save(admin);
        String adminLoginJson = om.writeValueAsString(loginRequest(admin.getEmail(), "Admin@123"));

        String adminToken = loginAnd_getToken(adminLoginJson);

        mvc.perform(patch("/api/v1/admin/users/" + owner.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(UserStatus.BLOCKED))
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/restaurants/" + restaurant.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }
    @Test
    void blockedUser_oldToken_returns401() throws Exception {
        User customer = User.builder()
                .name("customer")
                .password(passwordEncoder.encode("Abcd@1234"))
                .email("customer@test.com")
                .phone("8756746359")
                .role(Role.CUSTOMER).build();
        userRepository.save(customer);
        String customerLoginJson = om.writeValueAsString(loginRequest(customer.getEmail(), "Abcd@1234"));
        String customerToken = loginAnd_getToken(customerLoginJson);

        mvc.perform(get("/api/v1/user/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.email").value(customer.getEmail()));

        User admin = User.builder()
                .name("admin")
                .password(passwordEncoder.encode("Admin@123"))
                .email("admin@test.com")
                .phone("9856567878")
                .role(Role.ADMIN).build();
        userRepository.save(admin);
        String adminLoginJson = om.writeValueAsString(loginRequest(admin.getEmail(), "Admin@123"));

        String adminToken = loginAnd_getToken(adminLoginJson);

        mvc.perform(patch("/api/v1/admin/users/" + customer.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(UserStatus.BLOCKED))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));

    }
}
