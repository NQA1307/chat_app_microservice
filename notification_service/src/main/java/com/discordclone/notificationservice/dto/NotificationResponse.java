package com.discordclone.notificationservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private UUID recipientId;
    private String type;
    private String title;
    private String body;
    private boolean read;
    private String sourceType;
    private String sourceId;
    private LocalDateTime createdAt;
}
