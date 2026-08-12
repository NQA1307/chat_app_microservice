package com.discordclone.common.event;

import java.util.UUID;

public record UserBlockEvent(
    UUID blockerId,
    UUID blockedId,
    boolean blocked // true = blocked, false = unblocked
) {}
