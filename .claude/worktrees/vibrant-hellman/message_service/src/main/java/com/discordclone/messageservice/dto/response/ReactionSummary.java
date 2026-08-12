package com.discordclone.messageservice.dto.response;

public record ReactionSummary(
    String emoji,
    long count
) {}