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
    private String role;
    private LocalDateTime createdAt;
}
