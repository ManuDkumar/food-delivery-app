package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.dto.AuthResponse;
import dev.manu.food_delivery_app.dto.LoginRequest;
import dev.manu.food_delivery_app.dto.RegisterRequest;
import dev.manu.food_delivery_app.entity.User;
import dev.manu.food_delivery_app.enums.Role;
import dev.manu.food_delivery_app.enums.UserStatus;
import dev.manu.food_delivery_app.repository.UserRepository;
import dev.manu.food_delivery_app.security.JWTUtility;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JWTUtility jwtUtility;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthResponse customerRegister(RegisterRequest registerRequest) {
        if(userRepository.findByEmail(registerRequest.email()).isPresent()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = User.builder()
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .name(registerRequest.name())
                .phone(registerRequest.phone())
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);


        return new AuthResponse(jwtUtility.generateToken(
                user.getEmail()), user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse restaurantRegister(RegisterRequest registerRequest) {
        if(userRepository.findByEmail(registerRequest.email()).isPresent()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = User.builder()
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .name(registerRequest.name())
                .phone(registerRequest.phone())
                .role(Role.RESTAURANT_OWNER)
                .build();

        userRepository.save(user);


        return new AuthResponse(jwtUtility.generateToken(
                user.getEmail()), user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse registerAdmin(RegisterRequest registerRequest) {
        if(userRepository.findByEmail(registerRequest.email()).isPresent()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = User.builder()
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .name(registerRequest.name())
                .phone(registerRequest.phone())
                .role(Role.ADMIN)
                .build();

        userRepository.save(user);


        return new AuthResponse(jwtUtility.generateToken(
                user.getEmail()), user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );

        User user = userRepository.findByEmail(loginRequest.email()).orElseThrow(
                () -> new BadCredentialsException("Invalid email or password")
        );

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        return new AuthResponse(jwtUtility.generateToken(
                user.getEmail()), user.getName(), user.getEmail(), user.getRole().name());
    }
}
