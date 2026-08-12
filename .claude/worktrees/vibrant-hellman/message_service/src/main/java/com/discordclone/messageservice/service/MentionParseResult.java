package com.discordclone.messageservice.service;

import java.util.List;
import java.util.UUID;

public record MentionParseResult(
        List<UUID> userIds,
        boolean mentionEveryone,
        boolean mentionHere
) {}