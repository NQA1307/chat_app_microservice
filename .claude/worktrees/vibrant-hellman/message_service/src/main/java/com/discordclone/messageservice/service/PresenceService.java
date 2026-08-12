package com.discordclone.messageservice.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.discordclone.messageservice.dto.response.PresenceStatusResponse;
import com.discordclone.messageservice.client.UserServiceClient;
import com.discordclone.messageservice.client.ServerServiceClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserServiceClient userServiceClient;
    private final ServerServiceClient serverServiceClient;

    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final Duration OFFLINE_TTL = Duration.ofHours(24);
    public void markOnline(UUID userId, String sessionId) {
        String sessionsKey = sessionKey(userId);
        String statusKey = statusKey(userId);
        String now = LocalDateTime.now().toString();

        Long before = redisTemplate.opsForSet().size(sessionsKey);

        redisTemplate.opsForSet().add(sessionsKey, sessionId);
        redisTemplate.expire(sessionsKey, SESSION_TTL);

        redisTemplate.opsForHash().put(statusKey, "status", "ONLINE");
        redisTemplate.opsForHash().put(statusKey, "lastSeenAt", now);
        redisTemplate.expire(statusKey, SESSION_TTL);

        if (before == null || before == 0) {
            broadcast(userId, "ONLINE", LocalDateTime.parse(now));
        }
    }
    
    //Danh dau nguoi dung offline
    public void markOffline(UUID userId, String sessionId) {
        String sessionsKey = sessionKey(userId);
        String statusKey = statusKey(userId);

        redisTemplate.opsForSet().remove(sessionsKey, sessionId);

        Long remaining = redisTemplate.opsForSet().size(sessionsKey);
        if (remaining != null && remaining > 0) {
            return;
        }

        String now = LocalDateTime.now().toString();

        redisTemplate.delete(sessionsKey);
        redisTemplate.opsForHash().put(statusKey, "status", "OFFLINE");
        redisTemplate.opsForHash().put(statusKey, "lastSeenAt", now);
        redisTemplate.expire(statusKey, OFFLINE_TTL);

        broadcast(userId, "OFFLINE", LocalDateTime.parse(now));
    }


    //Lay trang thai nguoi dung
    public PresenceStatusResponse getStatus(UUID userId) {
        String key = statusKey(userId);
        Object status = redisTemplate.opsForHash().get(key, "status");
        Object lastSeenAt = redisTemplate.opsForHash().get(key, "lastSeenAt");


        String statusValue = status == null ? "OFFLINE" : status.toString();
        LocalDateTime lastSeenAtValue = lastSeenAt == null
                ? null
                : LocalDateTime.parse(lastSeenAt.toString());

        return  PresenceStatusResponse.builder()
            .userId(userId)
            .status(statusValue)
            .lastSeenAt(lastSeenAtValue)
            .build();
    }

    public List<PresenceStatusResponse> getStatuses(List<UUID> userIds) {
        return userIds.stream()
                .distinct()
                .map(this::getStatus)
                .toList();
    }

    //
    private void broadcast(UUID userId, String status, LocalDateTime lastSeenAt) {
        // 1. Gather potential recipients (friends + server co-members)
        Set<UUID> candidateIds = new HashSet<>();
        try {
            candidateIds.addAll(userServiceClient.getFriendIds(userId));
        } catch (Exception e) {
            // fallback handled by client
        }
        try {
            candidateIds.addAll(serverServiceClient.getCoMemberIds(userId));
        } catch (Exception e) {
            // fallback handled by client
        }

        // 2. Filter out candidates that have blocked or are blocked by userId
        List<UUID> allowedRecipients = new ArrayList<>();
        if (!candidateIds.isEmpty()) {
            try {
                allowedRecipients = userServiceClient.filterBlockedPresence(userId, new ArrayList<>(candidateIds));
            } catch (Exception e) {
                // fallback handled by client
            }
        }

        PresenceStatusResponse presenceUpdate = PresenceStatusResponse.builder()
                .userId(userId)
                .status(status)
                .lastSeenAt(lastSeenAt)
                .build();

        // 3. Send presence updates to each user-specific queue individually
        for (UUID recipientId : allowedRecipients) {
            messagingTemplate.convertAndSend("/topic/user." + recipientId + ".presence", presenceUpdate);
        }
    }

    public List<UUID> filterOnlineUsers(List<UUID> userIds) {
        return userIds.stream()
        .filter(this::isOnline)
        .toList();
    }

    public boolean isOnline(UUID userId){
        Object status = redisTemplate.opsForHash().get(statusKey(userId), "status");
        String statusValue = status == null ? "OFFLINE" : status.toString();
        return "ONLINE".equalsIgnoreCase(statusValue)
                || "IDLE".equalsIgnoreCase(statusValue)
                || "DO_NOT_DISTURB".equalsIgnoreCase(statusValue);
    }

    private String sessionKey(UUID userId) {
        return "presence:sessions:" + userId;
    }
    private String statusKey(UUID userId) {
        return "presence:user:" + userId;
    }
}
