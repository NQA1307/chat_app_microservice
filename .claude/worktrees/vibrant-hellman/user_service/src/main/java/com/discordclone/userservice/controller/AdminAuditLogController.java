package com.discordclone.userservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.userservice.dto.response.AdminAuditLogResponse;
import com.discordclone.userservice.entity.AdminAuditLog;
import com.discordclone.userservice.repository.AdminAuditLogRepository;
import com.discordclone.userservice.security.AdminGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AdminGuard adminGuard;
    private final AdminAuditLogRepository repository;

    @GetMapping
    public ApiResponse<Page<AdminAuditLogResponse>> getAuditLogs(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        adminGuard.requireAdmin(adminId);

        Page<AdminAuditLog> logs = repository.findByOrderByCreatedAtDesc(
                PageRequest.of(page, size)
        );

        return ApiResponse.success(logs.map(this::toResponse));
    }

    private AdminAuditLogResponse toResponse(AdminAuditLog log) {
        return AdminAuditLogResponse.builder()
                .id(log.getId())
                .adminUserId(log.getAdminUserId())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .reason(log.getReason())
                .metadata(log.getMetadata())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
