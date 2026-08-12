package com.discordclone.messageservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
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
    private String fileName;
    private Long fileSize;
    private String contentType;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String replyToMessageId;
    private ReplyPreview replyTo;
    private List<ReactionSummary> reactions;
    private String myReaction;
    private String messageType;
    private String metadata;
}
