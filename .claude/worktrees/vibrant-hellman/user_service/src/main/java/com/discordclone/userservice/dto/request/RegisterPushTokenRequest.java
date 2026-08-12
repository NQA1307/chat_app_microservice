package com.discordclone.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterPushTokenRequest(
    @NotBlank
    @Pattern(regexp = "^(ExponentPushToken|ExpoPushToken)\\[[A-Za-z0-9_-]+]$")
    String pushToken,
    @NotBlank String deviceId
) {}
