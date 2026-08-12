package com.discordclone.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterDeviceKeyRequest(
    @NotBlank String deviceId,
    @NotBlank String publicKey,
    String algorithm
) {}
