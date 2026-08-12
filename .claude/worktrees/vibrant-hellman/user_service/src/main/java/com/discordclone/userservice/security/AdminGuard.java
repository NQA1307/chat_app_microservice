package com.discordclone.userservice.security;

import com.discordclone.common.exception.AppException;
import com.discordclone.userservice.entity.User;
import com.discordclone.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminGuard {

    private final UserRepository userRepository;

    public User requireAdmin(UUID adminUserId) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Admin user not found"));

        if (admin.getRole() != User.Role.ADMIN && admin.getRole() != User.Role.SUPER_ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "Admin permission required");
        }

        return admin;
    }

    public User requireSuperAdmin(UUID adminUserId) {
        User admin = requireAdmin(adminUserId);

        if (admin.getRole() != User.Role.SUPER_ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "Super admin permission required");
        }

        return admin;
    }
}
