package com.discordclone.messageservice.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Builder
@Getter
public class MessageResponse {

    private String id;
    private Long channelId;
    private UUID senderId;
    private String senderUsername;
    private String content;
    private boolean deleted;
    private LocalDateTime createdAt;
}
