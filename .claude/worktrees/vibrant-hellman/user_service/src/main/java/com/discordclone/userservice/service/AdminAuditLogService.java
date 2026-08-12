package com.discordclone.userservice.service;

import com.discordclone.userservice.entity.AdminAuditLog;
import com.discordclone.userservice.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AdminAuditLogRepository repository;

    public void record(
            UUID adminId,
            String action,
            String targetType,
            String targetId,
            String reason,
            String metadata,
            String ipAddress
    ) {
        repository.save(AdminAuditLog.builder()
                .id(UUID.randomUUID())
                .adminUserId(adminId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .metadata(metadata)
                .ipAddress(ipAddress)
                .build());
    }
}
