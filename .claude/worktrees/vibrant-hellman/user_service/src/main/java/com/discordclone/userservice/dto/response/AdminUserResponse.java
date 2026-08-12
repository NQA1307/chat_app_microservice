package com.discordclone.userservice.dto.response;

import com.discordclone.userservice.entity.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private UUID id;
    private String username;
    private String email;
    private boolean emailVerified;
    private User.Role role;
    private boolean locked;
    private boolean banned;
    private String banReason;
    private LocalDateTime createdAt;
    private LocalDateTime lockedAt;
    private LocalDateTime bannedAt;
}
