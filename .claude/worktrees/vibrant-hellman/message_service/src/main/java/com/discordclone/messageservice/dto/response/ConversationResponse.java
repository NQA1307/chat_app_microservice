package com.discordclone.messageservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private Long id;
    private UUID participantId1;
    private UUID participantId2;
    private LocalDateTime createdAt;
}
