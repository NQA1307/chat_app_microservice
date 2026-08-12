package com.discordclone.userservice.dto.response;

import java.util.UUID;

public record DevicePublicKeyResponse(
    UUID userId,
    String deviceId,
    String publicKey,
    String algorithm
) {}
