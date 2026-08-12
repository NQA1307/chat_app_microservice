package com.discordclone.messageservice.dto.response;

import lombok.Builder;
import java.time.Instant;
import java.util.UUID;

@Builder
public record TypingIndicatorEvent(
    String scope,
    String targetId,
    UUID userId,
    String username,
    boolean typing,
    Instant expiresAt
) {}
