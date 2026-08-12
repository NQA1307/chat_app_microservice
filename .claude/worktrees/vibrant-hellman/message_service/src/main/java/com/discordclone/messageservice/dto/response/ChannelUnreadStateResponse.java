package com.discordclone.messageservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChannelUnreadStateResponse {
    private Long channelId;
    private boolean unread;
    private long unreadCount;
    private long mentionCount;
    private String lastReadMessageId;
    private String latestMessageId;
}