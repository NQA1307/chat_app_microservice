package com.discordclone.common.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSentEvent {
    private String messageId;
    private Long channelId;
    private UUID senderId;
    private String senderUsername;
    private String content;
    private LocalDateTime createdAt;
    private List<UUID> recipientIds;
}   
