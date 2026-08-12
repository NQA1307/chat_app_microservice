package com.discordclone.messageservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetReactionRequest(
    @NotBlank
    @Size(max = 100)
    String emoji
) {}