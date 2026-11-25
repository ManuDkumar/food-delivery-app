package dev.manu.user_service.service;

import dev.manu.user_service.dto.user.ProfileResponse;
import dev.manu.user_service.dto.user.ProfileUpdateRequest;
import dev.manu.user_service.entity.User;
import dev.manu.user_service.enums.UserStatus;
import dev.manu.user_service.repository.UserRepository;
import dev.manu.user_service.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public ProfileResponse getProfile() {
        return toResponse(loadCurrentUser());
    }

    public ProfileResponse updateProfile(ProfileUpdateRequest request) {
        User user = loadCurrentUser();

        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }

        try {
            return toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already exists");
        }
    }
    public void deleteProfile() {
        User user = loadCurrentUser();

        user.setUserStatus(UserStatus.DELETED);
        user.setEmail("deleted_" + user.getId() + "@no-reply.local");
        user.setPhone(user.getId().toString().replace("-", ""));

        userRepository.save(user);
    }

    private User loadCurrentUser() {
        UUID userId = CurrentUser.get().userId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found"));
    }

    private ProfileResponse toResponse(User user) {
        return new ProfileResponse(
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name());
    }
}
