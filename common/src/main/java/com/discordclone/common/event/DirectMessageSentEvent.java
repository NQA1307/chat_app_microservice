package com.discordclone.common.event;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageSentEvent(
    String messageId,
    Long conversationId,
    UUID senderId,
    UUID receiverId,
    String content,
    Instant createdAt
) {} 
