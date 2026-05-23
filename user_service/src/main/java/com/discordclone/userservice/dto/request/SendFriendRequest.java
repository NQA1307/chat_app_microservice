package com.discordclone.userservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SendFriendRequest {

    @NotNull(message = "Target user ID is required")
    private UUID targetUserId;
}
