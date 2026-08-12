package com.discordclone.voiceservice.dto.response;

import java.util.UUID;

public record VoiceTokenResponse(
        String livekitUrl,
        String token,
        String roomName,
        int maxParticipants,
        UUID e2eeKeyOwnerUserId,
        Integer currentKeyVersion
) {
}
