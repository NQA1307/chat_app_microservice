package com.discordclone.messageservice.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class SendDirectMessageRequest {
    private Long conversationId;
    private UUID senderId;
    private String senderUsername;

    private String content;

    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String contentType;

    private String messageType;
    private String metadata;

    private String replyToMessageId;

}
