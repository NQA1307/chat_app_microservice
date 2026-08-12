package com.discordclone.serverservice.dto.response;

import com.discordclone.common.enums.InviteStatus;
import com.discordclone.common.enums.InviteType;

import java.time.LocalDateTime;
import java.util.UUID;

public record InviteResponse(
        String id,
        UUID senderId,
        UUID receiverId,
        InviteType type,
        InviteStatus status,
        String targetType,
        String targetId,
        String targetName,
        String message,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {}
