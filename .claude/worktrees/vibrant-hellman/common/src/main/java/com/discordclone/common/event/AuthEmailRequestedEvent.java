package com.discordclone.common.event;

import java.time.LocalDateTime;

public record AuthEmailRequestedEvent(
        String email,
        String type,
        String code,
        int expiresInMinutes,
        LocalDateTime createdAt
) {
}
