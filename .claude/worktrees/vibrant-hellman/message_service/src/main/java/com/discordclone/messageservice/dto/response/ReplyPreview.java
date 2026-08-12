package com.discordclone.messageservice.dto.response;

import java.util.UUID;

public record ReplyPreview(
    String id,
    UUID senderId,
    String senderUsername,
    String content,
    boolean deleted
) {}