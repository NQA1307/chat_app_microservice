package com.discordclone.serverservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ServerMemberResponse(
        Long id,
        Long serverId,
        UUID userId,
        String role,
        LocalDateTime joinedAt
) {}