package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.dto.ProfileResponse;
import dev.manu.food_delivery_app.dto.ProfileUpdateRequest;
import dev.manu.food_delivery_app.entity.User;
import dev.manu.food_delivery_app.enums.UserStatus;
import dev.manu.food_delivery_app.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public ProfileResponse getProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException(HttpStatus.NOT_FOUND + email + " not found")
        );


        return new ProfileResponse(
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name());
    }

    public ProfileResponse updateProfile(ProfileUpdateRequest profileUpdateRequest) {
        String currentUser = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(currentUser).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (profileUpdateRequest.name() != null) {
            user.setName(profileUpdateRequest.name());
        }
        if (profileUpdateRequest.phone() != null) {
            user.setPhone(profileUpdateRequest.phone());
        }

        User updatedUser;

        try {
            updatedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new  ResponseStatusException(HttpStatus.CONFLICT, "Phone already exists");
        }
        return new ProfileResponse(
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getPhone(),
                updatedUser.getRole().name());
    }

    public void deleteProfile() {
        String currentUser = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(currentUser).orElse(null);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        user.setUserStatus(UserStatus.DELETED);
        user.setEmail("deleted_" + user.getId() + "@no-reply.local");
        user.setPhone(user.getId().toString().replace("-", ""));
        userRepository.save(user);
    }
}
