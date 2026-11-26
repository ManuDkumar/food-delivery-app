package dev.manu.user_service.service;

import dev.manu.user_service.dto.admin.AdminUserResponse;
import dev.manu.user_service.entity.User;
import dev.manu.user_service.enums.UserStatus;
import dev.manu.user_service.repository.UserRepository;
import dev.manu.user_service.security.CurrentUser;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    @Transactional
    public void updateStatus(UUID userId, UserStatus userStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + userId));

        if (user.getEmail().equals(CurrentUser.get().email()) && userStatus != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin can not change status to " + userStatus);
        }

        if (user.getUserStatus().equals(userStatus)) {
            return;
        }
        user.setUserStatus(userStatus);
        userRepository.save(user);
    }
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse).toList();
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getUserStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
