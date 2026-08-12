package com.discordclone.serverservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.discordclone.common.enums.ModerationAction;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModerationLogResponse {
    private Long id;
    private Long serverId;
    private UUID actorId;
    private UUID targetUserId;
    private ModerationAction action;
    private String reason;
    private LocalDateTime createdAt;
}
