package com.discordclone.messageservice.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Data
@Builder
@Getter
public class MessageResponse {

    private String id;
    private Long channelId;
    private UUID senderId;
    private String senderUsername;
    private String content;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private boolean deleted;
    private LocalDateTime createdAt;
    private String replyToMessageId;
    private ReplyPreview replyTo;
    private List<ReactionSummary> reactions;
    private String myReaction;
    private String messageType;
    private String metadata;
}
