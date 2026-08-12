package com.discordclone.messageservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionCacheService {
    private final StringRedisTemplate redisTemplate;

    @Value("${permission.cache.ttl-seconds:300}")
    private long ttlSeconds;



    public Optional<Boolean> getChannelAccess(Long channelId, UUID userId) {
        String value = redisTemplate.opsForValue().get(key(channelId, userId));
        if (value == null)
        {
            return Optional.empty();
        }
        return Optional.of(Boolean.parseBoolean(value));
    }

    public void cacheChannelAccess(Long channelId, UUID userId, boolean allowed) {
        redisTemplate.opsForValue().set(
            key(channelId, userId),
            Boolean.toString(allowed),
            Duration.ofSeconds(ttlSeconds)
        );
    }

    private String key(Long channelId, UUID userId) {
        return "permission:channel:" + channelId + ":user:" + userId;
    }
}
