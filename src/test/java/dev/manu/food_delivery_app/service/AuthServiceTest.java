package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.dto.auth.AuthResponse;
import dev.manu.food_delivery_app.dto.auth.LoginRequest;
import dev.manu.food_delivery_app.dto.user.RegisterRequest;
import dev.manu.food_delivery_app.entity.User;
import dev.manu.food_delivery_app.enums.Role;
import dev.manu.food_delivery_app.enums.UserStatus;
import dev.manu.food_delivery_app.repository.UserRepository;
import dev.manu.food_delivery_app.security.JWTUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    UserRepository userRepository;
    @Mock
    private JWTUtility jwtUtility;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    AuthService authService;

    private User user(RegisterRequest registerRequest) {
        return User.builder()
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .role(Role.CUSTOMER)
                .phone(registerRequest.phone())
                .name(registerRequest.name())
                .build();
    }

    @Test
    void customerRegister_duplicateEmail_throws409() {
            RegisterRequest request = new RegisterRequest("customer", "customer@test.com", "Abcd@123", "9676767656");
            User user = user(request);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.customerRegister(request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Email already registered");
    }

    @Test
    void customerRegister_encodesPassword_returnsToken() {
        RegisterRequest request = new RegisterRequest("customer", "customer@test.com", "Abcd@123", "9676767656");

        when(passwordEncoder.encode(request.password())).thenReturn("HASHED");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        authService.customerRegister(request);

        verify(userRepository).saveAndFlush(captor.capture());

        assertThat(captor.getValue().getPassword()).isEqualTo("HASHED");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.CUSTOMER);
    }
    @Test
    void customerRegister_dbRace_throws409EmailOrPhone() {
        RegisterRequest request = new RegisterRequest("customer", "customer@test.com", "Abcd@123", "9676767656");

        when(userRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("conflict of email or phone uniqueness"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.customerRegister(request));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).isEqualTo("Email or phone already registered");
        verify(userRepository, times(1)).saveAndFlush(any());
    }
    @Test
    void login_success_returnsToken() {
        RegisterRequest reg = new RegisterRequest("customer", "customer@test.com", "Abcd@123", "9676767656");
        User user = user(reg);
        user.setUserStatus(UserStatus.ACTIVE);
        LoginRequest request = new LoginRequest("customer@test.com", "Abcd@123");
        Authentication auth = new UsernamePasswordAuthenticationToken(request.email(), request.password(), List.of());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(jwtUtility.generateToken(any())).thenReturn("Token");

        AuthResponse response = authService.login(request);
        verify(userRepository, times(1)).findByEmail(request.email());
        assertThat(response.token()).isEqualTo("Token");
    }

    @Test
    void login_inactiveAccount_throws403() {
        User blockedUser = User.builder()
                .email("customer@test.com")
                .userStatus(UserStatus.BLOCKED).build();
        LoginRequest request = new LoginRequest("customer@test.com", "Abcd@123");

        when(userRepository.findByEmail(blockedUser.getEmail())).thenReturn(Optional.of(blockedUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).isEqualTo("Account is not active");
    }

    @Test
    void login_wrongPassword_return401() {
        LoginRequest request = new LoginRequest("customer@test.com", "Abcd@123");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request));

        assertThat(ex.getMessage()).isEqualTo("Bad credentials");

        verify(userRepository, never()).findByEmail(any());
    }

}
