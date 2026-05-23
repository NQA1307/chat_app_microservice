package com.discordclone.messageservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class SendDirectMessageRequest {
    private Long conversationId;
    private UUID senderId;
    private String senderUsername;

    @NotBlank(message = "Content cannot be empty")
    private String content;
}
