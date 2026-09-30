package dev.manu.food_delivery_app.service;

import dev.manu.food_delivery_app.entity.User;
import dev.manu.food_delivery_app.enums.UserStatus;
import dev.manu.food_delivery_app.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AdminService {

    private final UserRepository userRepository;


    public void updateStatus(UUID userId, UserStatus userStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + userId));

        String currentUser = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        if (user.getEmail().equals(currentUser) && userStatus == UserStatus.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin can not block themself");
        }

        if (user.getUserStatus().equals(userStatus)) {
            return;
        }
        user.setUserStatus(userStatus);
        userRepository.save(user);
    }
}
