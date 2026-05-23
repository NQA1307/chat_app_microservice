package com.discordclone.messageservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceStatusResponse {
    private UUID userId;
    private String status;
    private LocalDateTime lastSeenAt;
}
