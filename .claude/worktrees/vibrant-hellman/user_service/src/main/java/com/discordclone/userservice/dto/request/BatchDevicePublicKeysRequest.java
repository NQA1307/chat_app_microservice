package com.discordclone.userservice.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record BatchDevicePublicKeysRequest(
    @NotEmpty List<UUID> userIds
) {}
