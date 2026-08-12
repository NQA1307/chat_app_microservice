package com.discordclone.common.event;

import java.util.UUID;

public record FriendRequestEvent(
    UUID senderId,
    String senderUsername,
    UUID receiverId,
    String type // SENT, ACCEPTED
) {}
