package com.discordclone.serverservice.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

import com.discordclone.common.enums.MuteType;

import lombok.Data;

@Data
public class MuteMemberRequest {
    private UUID userId;
    private Long channelId;
    private MuteType type;
    private LocalDateTime expiresAt;
    private String reason;
}
