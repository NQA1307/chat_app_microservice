package com.discordclone.common.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DirectMessageSentEvent(
    String eventId,
    String messageId,
    Long conversationId,
    UUID senderId,
    UUID receiverId,
    String preview,
    String content,
    List<UUID> mentionedUserIds,
    Instant createdAt
) {} 
