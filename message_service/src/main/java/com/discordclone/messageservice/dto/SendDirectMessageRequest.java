package com.discordclone.messageservice.dto;

import lombok.Data;

@Data
public class SendDirectMessageRequest {
    private Long conversationId;
    private Long senderId;
    private String senderUsername;
    private String content;
}
