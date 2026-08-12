package com.discordclone.messageservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TypingIndicatorRequest(
    @NotBlank String scope,
    @NotBlank String targetId,
    boolean typing
) {}