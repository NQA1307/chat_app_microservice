package com.discordclone.serverservice.dto.request;

import java.util.UUID;

public record SendServerInviteRequest (
    UUID receiverUserId,
    String message
) {}
