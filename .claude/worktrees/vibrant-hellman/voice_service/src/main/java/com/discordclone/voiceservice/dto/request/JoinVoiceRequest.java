package com.discordclone.voiceservice.dto.request;

import jakarta.validation.constraints.NotNull;

public record JoinVoiceRequest(
        @NotNull Long serverId
) {
}
