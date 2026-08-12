package com.discordclone.userservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.common.exception.AppException;
import com.discordclone.userservice.dto.request.AdminReasonRequest;
import com.discordclone.userservice.dto.request.UpdateUserRoleRequest;
import com.discordclone.userservice.dto.response.AdminUserResponse;
import com.discordclone.userservice.entity.User;
import com.discordclone.userservice.repository.UserRepository;
import com.discordclone.userservice.security.AdminGuard;
import com.discordclone.userservice.service.AdminAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminGuard adminGuard;
    private final UserRepository userRepository;
    private final AdminAuditLogService auditLogService;

    @GetMapping
    public ApiResponse<Page<AdminUserResponse>> searchUsers(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        adminGuard.requireAdmin(adminId);

        Page<User> users = userRepository.searchAdminUsers(q, PageRequest.of(page, size));
        return ApiResponse.success(users.map(this::toResponse));
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserResponse> getUser(
            @RequestHeader("X-User-Id") UUID adminId,
            @PathVariable UUID userId
    ) {
        adminGuard.requireAdmin(adminId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        return ApiResponse.success(toResponse(user));
    }

    @PatchMapping("/{userId}/role")
    public ApiResponse<AdminUserResponse> updateRole(
            @RequestHeader("X-User-Id") UUID adminId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest request,
            HttpServletRequest httpRequest
    ) {
        User admin = adminGuard.requireSuperAdmin(adminId);

        if (adminId.equals(userId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot update your own role");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        User.Role oldRole = user.getRole();
        user.setRole(request.role());
        userRepository.save(user);

        String metadata = String.format("{\"oldRole\":\"%s\",\"newRole\":\"%s\"}", oldRole.name(), request.role().name());

        auditLogService.record(
                adminId,
                "USER_ROLE_UPDATED",
                "USER",
                userId.toString(),
                request.reason(),
                metadata,
                httpRequest.getRemoteAddr()
        );

        return ApiResponse.success(toResponse(user));
    }

    @PatchMapping("/{userId}/ban")
    public ApiResponse<AdminUserResponse> banUser(
            @RequestHeader("X-User-Id") UUID adminId,
            @PathVariable UUID userId,
            @RequestBody AdminReasonRequest request,
            HttpServletRequest httpRequest
    ) {
        User admin = adminGuard.requireAdmin(adminId);

        if (adminId.equals(userId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot ban yourself");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        // Only Super Admin can ban other administrative users
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN) {
            if (admin.getRole() != User.Role.SUPER_ADMIN) {
                throw new AppException(HttpStatus.FORBIDDEN, "Only Super Admin can ban administrative users");
            }
        }

        user.setBanned(true);
        user.setBanReason(request != null ? request.reason() : null);
        user.setBannedAt(LocalDateTime.now());
        userRepository.save(user);

        String metadata = String.format("{\"banned\":true}");

        auditLogService.record(
                adminId,
                "USER_BANNED",
                "USER",
                userId.toString(),
                request != null ? request.reason() : null,
                metadata,
                httpRequest.getRemoteAddr()
        );

        return ApiResponse.success(toResponse(user));
    }

    @PatchMapping("/{userId}/unban")
    public ApiResponse<AdminUserResponse> unbanUser(
            @RequestHeader("X-User-Id") UUID adminId,
            @PathVariable UUID userId,
            @RequestBody(required = false) AdminReasonRequest request,
            HttpServletRequest httpRequest
    ) {
        User admin = adminGuard.requireAdmin(adminId);

        if (adminId.equals(userId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot unban yourself");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        // Only Super Admin can unban other administrative users
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN) {
            if (admin.getRole() != User.Role.SUPER_ADMIN) {
                throw new AppException(HttpStatus.FORBIDDEN, "Only Super Admin can unban administrative users");
            }
        }

        user.setBanned(false);
        user.setBanReason(null);
        user.setBannedAt(null);
        userRepository.save(user);

        String metadata = String.format("{\"banned\":false}");

        auditLogService.record(
                adminId,
                "USER_UNBANNED",
                "USER",
                userId.toString(),
                request != null ? request.reason() : null,
                metadata,
                httpRequest.getRemoteAddr()
        );

        return ApiResponse.success(toResponse(user));
    }

    @PatchMapping("/{userId}/lock")
    public ApiResponse<AdminUserResponse> lockUser(
            @RequestHeader("X-User-Id") UUID adminId,
            @PathVariable UUID userId,
            @RequestBody(required = false) AdminReasonRequest request,
            HttpServletRequest httpRequest
    ) {
        User admin = adminGuard.requireAdmin(adminId);

        if (adminId.equals(userId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot lock yourself");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        // Only Super Admin can lock other administrative users
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN) {
            if (admin.getRole() != User.Role.SUPER_ADMIN) {
                throw new AppException(HttpStatus.FORBIDDEN, "Only Super Admin can lock administrative users");
            }
        }

        user.setLocked(true);
        user.setLockedAt(LocalDateTime.now());
        userRepository.save(user);

        String metadata = String.format("{\"locked\":true}");

        auditLogService.record(
                adminId,
                "USER_LOCKED",
                "USER",
                userId.toString(),
                request != null ? request.reason() : null,
                metadata,
                httpRequest.getRemoteAddr()
        );

        return ApiResponse.success(toResponse(user));
    }

    @PatchMapping("/{userId}/unlock")
    public ApiResponse<AdminUserResponse> unlockUser(
            @RequestHeader("X-User-Id") UUID adminId,
            @PathVariable UUID userId,
            @RequestBody(required = false) AdminReasonRequest request,
            HttpServletRequest httpRequest
    ) {
        User admin = adminGuard.requireAdmin(adminId);

        if (adminId.equals(userId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot unlock yourself");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        // Only Super Admin can unlock other administrative users
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN) {
            if (admin.getRole() != User.Role.SUPER_ADMIN) {
                throw new AppException(HttpStatus.FORBIDDEN, "Only Super Admin can unlock administrative users");
            }
        }

        user.setLocked(false);
        user.setLockedAt(null);
        userRepository.save(user);

        String metadata = String.format("{\"locked\":false}");

        auditLogService.record(
                adminId,
                "USER_UNLOCKED",
                "USER",
                userId.toString(),
                request != null ? request.reason() : null,
                metadata,
                httpRequest.getRemoteAddr()
        );

        return ApiResponse.success(toResponse(user));
    }

    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .role(user.getRole())
                .locked(user.isLocked())
                .banned(user.isBanned())
                .banReason(user.getBanReason())
                .createdAt(user.getCreatedAt())
                .lockedAt(user.getLockedAt())
                .bannedAt(user.getBannedAt())
                .build();
    }
}
