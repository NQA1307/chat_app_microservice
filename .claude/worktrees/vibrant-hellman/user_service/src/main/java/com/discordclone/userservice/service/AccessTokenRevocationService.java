package com.discordclone.userservice.service;

import com.discordclone.userservice.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AccessTokenRevocationService {

    private static final String REVOKED_ACCESS_PREFIX = "revoked_access_token:";
    private static final String VALID_AFTER_PREFIX = "access_token_valid_after:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public void revokeToken(String authorizationHeader) {
        try {
            if (authorizationHeader == null || authorizationHeader.isBlank()) {
                return;
            }

            Claims claims = jwtUtil.parseClaims(authorizationHeader);
            if (!"access".equals(claims.get("type", String.class))) {
                return;
            }

            String tokenId = claims.getId();
            Date expiresAt = claims.getExpiration();
            if (tokenId == null || tokenId.isBlank() || expiresAt == null) {
                return;
            }

            long ttlMillis = expiresAt.getTime() - System.currentTimeMillis();
            if (ttlMillis > 0) {
                redisTemplate.opsForValue().set(REVOKED_ACCESS_PREFIX + tokenId, "1", ttlMillis, TimeUnit.MILLISECONDS);
            }
        } catch (RuntimeException ignored) {
            // Expired or malformed access tokens cannot be used and do not need a blacklist entry.
        }
    }

    public void revokeAllForUser(UUID userId) {
        redisTemplate.opsForValue().set(
                VALID_AFTER_PREFIX + userId,
                String.valueOf(Instant.now().toEpochMilli()),
                refreshExpiration,
                TimeUnit.MILLISECONDS
        );
    }
}
