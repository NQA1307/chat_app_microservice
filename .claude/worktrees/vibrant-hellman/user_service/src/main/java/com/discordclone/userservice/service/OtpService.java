package com.discordclone.userservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.discordclone.common.exception.AppException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.otp.secret}")
    private String otpSecret;

    @Value("${app.otp.ttl-minutes:10}")
    private long ttlMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long cooldownSeconds;

    private final SecureRandom secureRandom = new SecureRandom();


    //Tao ma otp
    public String createCode(String email, String purpose) {
        String normalizedEmail = normalizeEmail(email);
        String cooldownKey = cooldownKey(purpose, normalizedEmail);

        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting another code");
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        String hashed = hash(purpose, normalizedEmail, code);

        redisTemplate.opsForValue().set(codeKey(purpose, normalizedEmail), hashed, ttlMinutes, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(cooldownKey, "1", cooldownSeconds, TimeUnit.SECONDS);
        redisTemplate.delete(attemptsKey(purpose, normalizedEmail));

        return code;
    }


    //Xac thuc ma otp
    public void verifyCode(String email, String code, String purpose) {
        String normalizedEmail = normalizeEmail(email);
        String attemptsKey = attemptsKey(purpose, normalizedEmail);

        String attemptsValue = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsValue == null ? 0 : Integer.parseInt(attemptsValue);

        if (attempts >= maxAttempts) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "Too many invalid attempts");
        }

        String storedHash = redisTemplate.opsForValue().get(codeKey(purpose, normalizedEmail));
        if (storedHash == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Code expired or invalid");
        }

        String incomingHash = hash(purpose, normalizedEmail, code);

        if (!constantTimeEquals(storedHash, incomingHash)) {
            redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, ttlMinutes, TimeUnit.MINUTES);
            throw new AppException(HttpStatus.UNAUTHORIZED, "Code expired or invalid");
        }

        redisTemplate.delete(codeKey(purpose, normalizedEmail));
        redisTemplate.delete(attemptsKey);
    }


    //Bat dau session reset password
    public String createPasswordResetSession(UUID userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("password_reset_session:" + token, userId.toString(), 5, TimeUnit.MINUTES);
        return token;
    }


    //Comsume event 
    public UUID consumePasswordResetSession(String token) {
        String key = "password_reset_session:" + token;
        String userId = redisTemplate.opsForValue().get(key);

        if (userId == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Reset token expired or invalid");
        }

        redisTemplate.delete(key);
        return UUID.fromString(userId);
    }

    private String codeKey(String purpose, String email) {
        return purpose + ":" + email;
    }

    private String attemptsKey(String purpose, String email) {
        return purpose + ":attempts:" + email;
    }

    private String cooldownKey(String purpose, String email) {
        return purpose + ":cooldown:" + email;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String hash(String purpose, String email, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(otpSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((purpose + ":" + email + ":" + code).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not hash verification code");
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
