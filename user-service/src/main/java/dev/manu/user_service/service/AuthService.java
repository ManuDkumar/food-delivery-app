package dev.manu.user_service.service;

import dev.manu.user_service.dto.auth.AuthResponse;
import dev.manu.user_service.dto.auth.LoginRequest;
import dev.manu.user_service.dto.user.RegisterRequest;
import dev.manu.user_service.entity.User;
import dev.manu.user_service.enums.Role;
import dev.manu.user_service.enums.UserStatus;
import dev.manu.user_service.repository.UserRepository;
import dev.manu.user_service.security.JWTUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtility jwtUtility;

    public AuthResponse customerRegister(RegisterRequest request) {
        return register(request, Role.CUSTOMER);
    }

    public AuthResponse restaurantRegister(RegisterRequest request) {
        return register(request, Role.RESTAURANT_OWNER);
    }

    public AuthResponse registerAdmin(RegisterRequest request) {
        return register(request, Role.ADMIN);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        return issue(user);
    }
    private AuthResponse register(RegisterRequest request, Role role) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phone(request.phone())
                .role(role)
                .build();

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or phone already registered");
        }
        return issue(user);
    }

    private AuthResponse issue(User user) {
        return new AuthResponse(
                jwtUtility.generateToken(user),
                user.getName(),
                user.getEmail(),
                user.getRole().name());
    }
}
