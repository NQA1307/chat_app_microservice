package com.discordclone.messageservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectMessageResponse {
    private String id;
    private Long conversationId;
    private UUID senderId;
    private String senderUsername;
    private String content;
    private String fileUrl;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
