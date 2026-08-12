package com.discordclone.messageservice.service;

import com.discordclone.common.enums.PermissionCode;
import com.discordclone.messageservice.client.ServerServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionCacheService permissionCacheService;
    private final ServerServiceClient serverServiceClient;

    public boolean canAccessChannel(Long channelId, UUID userId) {
        return permissionCacheService.getChannelAccess(channelId, userId)
                .orElseGet(() -> {
                    // Cache miss: ask server-service, then keep the result briefly in Redis.
                    boolean allowed = serverServiceClient.canAccessChannel(channelId, userId);
                    permissionCacheService.cacheChannelAccess(channelId, userId, allowed);
                    return allowed;
                });
    }

    public boolean canManageMessages(Long channelId, UUID userId) {
        return serverServiceClient.hasChannelPermission(
                channelId,
                userId,
                PermissionCode.MANAGE_MESSAGES);
    }

    public boolean canSendMessage(Long channelId, UUID userId) {
        return serverServiceClient.canSendMessage(channelId, userId);
    }

    public boolean canMentionEveryone(Long channelId, UUID userId) {
        return serverServiceClient.hasChannelPermission(channelId, 
            userId,
             PermissionCode.MENTION_EVERYONE);
    }
}
