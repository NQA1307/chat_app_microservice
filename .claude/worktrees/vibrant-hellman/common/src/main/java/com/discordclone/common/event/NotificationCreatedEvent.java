package com.discordclone.common.event;

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
public class NotificationCreatedEvent {
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