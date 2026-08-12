package com.discordclone.userservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.userservice.dto.response.AdminDashboardResponse;
import com.discordclone.userservice.repository.UserRepository;
import com.discordclone.userservice.security.AdminGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminGuard adminGuard;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<AdminDashboardResponse> getDashboard(
            @RequestHeader("X-User-Id") UUID adminId
    ) {
        adminGuard.requireAdmin(adminId);

        LocalDateTime today = LocalDate.now().atStartOfDay();

        return ApiResponse.success(new AdminDashboardResponse(
                userRepository.count(),
                userRepository.countByEmailVerifiedTrue(),
                userRepository.countByBannedTrue(),
                userRepository.countByLockedTrue(),
                userRepository.countByCreatedAtAfter(today)
        ));
    }
}
