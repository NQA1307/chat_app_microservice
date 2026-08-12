package com.discordclone.messageservice.dto.response;

import java.util.List;

public record ReactionUpdateResponse(
    String sourceType,
    String sourceId,
    String myReaction,
    String action,
    List<ReactionSummary> reactions
) {}