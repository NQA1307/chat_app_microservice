package com.discordclone.common.event;

import java.util.UUID;

public record ServerMemberEvent(
    Long serverId,
    String serverName,
    UUID actorId,    // Người thực hiện (ví dụ người kick)
    UUID targetId,   // Người bị tác động (người bị kick/rời)
    String action    // KICKED, LEFT
) {}
