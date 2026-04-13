package com.discordclone.messageservice.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
@Builder
@Getter
public class MessageResponse {

    private Long id;
    private Long channelId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private LocalDateTime createdAt;
}
