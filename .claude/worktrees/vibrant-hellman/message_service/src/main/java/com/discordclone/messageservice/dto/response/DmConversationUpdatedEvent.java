package com.discordclone.messageservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DmConversationUpdatedEvent(
    Long conversationId,
    UUID senderId,
    UUID receiverId,
    String senderUsername,
    String lastMessagePreview,
    LocalDateTime lastMessageAt
) {}