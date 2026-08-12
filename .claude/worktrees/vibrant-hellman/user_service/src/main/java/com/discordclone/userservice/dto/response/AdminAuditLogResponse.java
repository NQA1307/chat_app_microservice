package com.discordclone.userservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLogResponse {
    private UUID id;
    private UUID adminUserId;
    private String action;
    private String targetType;
    private String targetId;
    private String reason;
    private String metadata;
    private String ipAddress;
    private LocalDateTime createdAt;
}
