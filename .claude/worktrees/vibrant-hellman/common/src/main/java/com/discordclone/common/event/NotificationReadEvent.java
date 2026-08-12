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
public class NotificationReadEvent {
    private String eventId;
    private UUID recipientId;
    private String notificationId;
    private String sourceType;
    private String sourceId;
    private long unreadCount;
    private LocalDateTime readAt;
}
