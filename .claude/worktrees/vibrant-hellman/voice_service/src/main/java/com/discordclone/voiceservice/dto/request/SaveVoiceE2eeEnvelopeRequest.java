package com.discordclone.voiceservice.dto.request;

import java.util.List;
import java.util.UUID;

public record SaveVoiceE2eeEnvelopeRequest(
        String roomName,
        Integer keyVersion,
        List<EnvelopeItem> envelopes
) {
    public record EnvelopeItem(
            UUID recipientUserId,
            String recipientDeviceId,
            String encryptedKey,
            String iv,
            String algorithm
    ) {}
}