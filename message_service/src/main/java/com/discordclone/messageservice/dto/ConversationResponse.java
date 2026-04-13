package com.discordclone.messageservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private Long id;
    private Long participantId1;
    private Long participantId2;
    private LocalDateTime createdAt;
}
