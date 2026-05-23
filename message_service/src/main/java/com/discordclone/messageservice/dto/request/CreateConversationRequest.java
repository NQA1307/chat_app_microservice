package com.discordclone.messageservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateConversationRequest {
    @NotNull(message = "Target user ID is required")
    private UUID targetUserId;
}
