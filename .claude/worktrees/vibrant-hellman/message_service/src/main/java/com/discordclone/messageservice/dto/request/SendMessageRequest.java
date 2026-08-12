package com.discordclone.messageservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SendMessageRequest {

    @NotNull(message = "Channel ID is required")
    private Long channelId;

    private String content;

    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String contentType;

    // Được set bởi server từ X-User-Id và X-User-Username headers
    private UUID senderId;
    private String senderUsername;

    private String messageType;
    private String metadata;

    private String replyToMessageId;
}
