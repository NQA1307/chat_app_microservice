package com.discordclone.userservice.service;

import com.discordclone.common.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PendingRegistrationService {

    private static final String KEY_PREFIX = "auth:pending-registration:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.registration.pending-ttl-minutes:30}")
    private long pendingTtlMinutes;

    public void save(PendingRegistration pendingRegistration) {
        try {
            redisTemplate.opsForValue().set(
                    key(pendingRegistration.email()),
                    objectMapper.writeValueAsString(pendingRegistration),
                    pendingTtlMinutes,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store pending registration");
        }
    }

    public PendingRegistration get(String email) {
        String value = redisTemplate.opsForValue().get(key(email));
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.readValue(value, PendingRegistration.class);
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read pending registration");
        }
    }

    public PendingRegistration require(String email) {
        PendingRegistration pendingRegistration = get(email);
        if (pendingRegistration == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Registration session expired");
        }
        return pendingRegistration;
    }

    public boolean exists(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(email)));
    }

    public void delete(String email) {
        redisTemplate.delete(key(email));
    }

    private String key(String email) {
        return KEY_PREFIX + email.trim().toLowerCase();
    }

    public record PendingRegistration(
            String email,
            String username,
            String displayName,
            String passwordHash
    ) {
    }
}
