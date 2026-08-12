package com.discordclone.messageservice.dto.response;

import java.util.List;
import java.util.UUID;

public record MessageReactionEvent(
    String eventId,
    String sourceType,
    String sourceId,
    Long channelId,
    Long conversationId,
    UUID actorUserId,
    String actorReaction,
    String action,
    List<ReactionSummary> reactions
) {}