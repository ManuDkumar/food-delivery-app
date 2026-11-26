package dev.manu.user_service.service;

import dev.manu.user_service.entity.User;
import dev.manu.user_service.enums.Role;
import dev.manu.user_service.enums.UserStatus;
import dev.manu.user_service.repository.UserRepository;
import dev.manu.user_service.security.CurrentUser;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private AdminService adminService;

    private final UUID adminId =  UUID.randomUUID();

    @BeforeEach
    void seedAdmin() {
        CurrentUser admin = new CurrentUser(
                adminId, "Admin@gmail.com","ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of())
        );
    }
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }
    @Test
    void updateStatus_unknownUser_throw404() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> adminService.updateStatus(id, UserStatus.BLOCKED)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getReason()).isEqualTo("User not found: " + id);
    }

    @Test
    void selfBlocked_throws409() {
        User admin = user(adminId,"Admin@gmail.com", Role.ADMIN, UserStatus.ACTIVE);

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> adminService.updateStatus(admin.getId(), UserStatus.BLOCKED));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Admin can not change status to " + UserStatus.BLOCKED);
    }

    @Test
    void sameStatus_doesNotSaves() {
        User user = user(UUID.randomUUID(), "user@test.com", Role.CUSTOMER, UserStatus.ACTIVE);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        adminService.updateStatus(user.getId(), UserStatus.ACTIVE);

        AssertionsForClassTypes.assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);

        verify(userRepository, never()).save(user);
    }

    @Test
    void blockCustomer_savesNewStatus() {
        User customer = user(UUID.randomUUID(), "customer",  Role.CUSTOMER, UserStatus.ACTIVE);
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        adminService.updateStatus(customer.getId(), UserStatus.BLOCKED);

        AssertionsForClassTypes.assertThat(customer.getUserStatus()).isEqualTo(UserStatus.BLOCKED);

        verify(userRepository).save(customer);
    }

    private User user(UUID id, String email, Role role, UserStatus
            status) {
        return User.builder()
                .id(id)
                .email(email)
                .role(role)
                .userStatus(status)
                .build();
    }
}
