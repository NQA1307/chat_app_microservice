package com.discordclone.common.event;

import java.time.Instant;
import java.util.UUID;

public record ServerInviteEvent(
    String inviteId,
    Long serverId,
    UUID senderId,
    UUID receiverId,
    String serverName,
    Instant createdAt
) {}
