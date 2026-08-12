package com.discordclone.userservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@Getter
@Setter
public class UserResponse {

    private java.util.UUID id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String bannerColor;
    private String customStatus;
    private String role;
    private boolean emailVerified;
    private LocalDateTime createdAt;
}
