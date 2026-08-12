package com.discordclone.voiceservice.dto.response;

public record VoiceE2eeEnvelopeResponse(
        String roomName,
        Long channelId,
        Integer keyVersion,
        String encryptedKey,
        String iv,
        String algorithm
) {}