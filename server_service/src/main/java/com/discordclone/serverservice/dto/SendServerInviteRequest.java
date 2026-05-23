package com.discordclone.serverservice.dto;

import java.util.UUID;

public record SendServerInviteRequest (
    UUID receiverUserId,
    String message
) {}
