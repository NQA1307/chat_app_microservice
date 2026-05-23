package com.discordclone.voiceservice.dto.response;

public record VoiceTokenResponse(
        String livekitUrl,
        String token,
        String roomName,
        int maxParticipants
) {
}
