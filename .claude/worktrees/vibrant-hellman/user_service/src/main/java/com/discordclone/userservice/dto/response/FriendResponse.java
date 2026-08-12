package com.discordclone.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendResponse {

    private UUID friendshipId;
    private UUID userId;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String status;
    private String direction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
