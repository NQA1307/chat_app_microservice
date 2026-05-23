package com.discordclone.messageservice.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.discordclone.messageservice.dto.response.PresenceStatusResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private static final Duration PRESENCE_TTL = Duration.ofSeconds(60);


    //Danh dau nguoi dung online
    public void markOnline(UUID userId) {
        String key = key(userId);
        String now = LocalDateTime.now().toString();

        redisTemplate.opsForHash().put(key, "status","ONLINE");
        redisTemplate.opsForHash().put(key, "lastSeenAt", now);
        redisTemplate.expire(key, PRESENCE_TTL);

        broadcast(userId, "ONLINE", LocalDateTime.parse(now));
    }
    
    //Danh dau nguoi dung offline
    public void markOffline(UUID userId) {
        String key = key(userId);
        String now = LocalDateTime.now().toString();

        redisTemplate.opsForHash().put(key, "status", "OFFLINE");
        redisTemplate.opsForHash().put(key, "lastSeenAt", now);
        redisTemplate.expire(key, Duration.ofHours(24));

        broadcast(userId, "OFFLINE", LocalDateTime.parse(now));    
    }


    //Lay trang thai nguoi dung
    public PresenceStatusResponse getStatus(UUID userId) {
        String key = key(userId);
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



    private String key(UUID userId) {
        return "presence:user:" + userId;
    }

    //
    private void broadcast(UUID userId, String status, LocalDateTime lastSeenAt) {
        messagingTemplate.convertAndSend(
            "/topic/presence",
            PresenceStatusResponse.builder()
                .userId(userId)
                .status(status)
                .lastSeenAt(lastSeenAt)
                .build()
        );
    }
}
