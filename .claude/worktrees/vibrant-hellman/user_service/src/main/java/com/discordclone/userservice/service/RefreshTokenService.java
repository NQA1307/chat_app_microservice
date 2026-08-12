package com.discordclone.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private static final String PREFIX = "refresh_token:";

    public void save(UUID userId, String tokenId, String refreshToken) {
        redisTemplate.opsForValue().set(
            key(userId, tokenId),
            refreshToken,
            refreshExpiration,
            TimeUnit.MILLISECONDS
        );
    }

    public boolean isValid(UUID userId, String tokenId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(key(userId, tokenId));
        if (stored == null) {
            stored = redisTemplate.opsForValue().get(PREFIX + userId);
        }
        return refreshToken.equals(stored);
    }

    public void revoke(UUID userId, String tokenId) {
        redisTemplate.delete(key(userId, tokenId));
        redisTemplate.delete(PREFIX + userId);
    }

    public void revokeAll(UUID userId) {
        Set<String> keys = redisTemplate.keys(PREFIX + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        redisTemplate.delete(PREFIX + userId);
    }

    private String key(UUID userId, String tokenId) {
        return PREFIX + userId + ":" + tokenId;
    }
}
