package com.discordclone.messageservice.dto;

import lombok.Data;

@Data
public class CreateConversationRequest {
    private Long targetUserId;
}
