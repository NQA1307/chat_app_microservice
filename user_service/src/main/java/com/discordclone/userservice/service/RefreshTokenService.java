package com.discordclone.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private static final String PREFIX = "refresh_token:";

    public void save(UUID userId, String refreshToken) {
        String key = PREFIX + userId.toString();
        redisTemplate.opsForValue().set(
            key,
            refreshToken,
            refreshExpiration,
            TimeUnit.MILLISECONDS
        );
    }

    public boolean isValid(UUID userId, String refreshToken) {
        String key = PREFIX + userId.toString();
        String stored = redisTemplate.opsForValue().get(key);
        return refreshToken.equals(stored);
    }

    // Xoa khi dang xuat
    public void revoke(UUID userId) {
        redisTemplate.delete(PREFIX + userId.toString());
    }
}
