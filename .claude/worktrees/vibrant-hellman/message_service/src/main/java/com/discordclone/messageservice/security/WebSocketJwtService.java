package com.discordclone.messageservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Service
public class WebSocketJwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer:discord-clone}")
    private String issuer;

    private final StringRedisTemplate redisTemplate;

    public WebSocketJwtService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public AuthenticatedUser parseAccessToken(String authorizationHeader) {
        String token = stripBearerPrefix(authorizationHeader);
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();

        if (!"access".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("JWT token type must be access");
        }

        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("JWT subject is missing");
        }

        if (isAccessTokenRevoked(claims, subject)) {
            throw new IllegalArgumentException("JWT access token was revoked");
        }

        String username = claims.get("username", String.class);
        return new AuthenticatedUser(
                UUID.fromString(subject),
                username == null || username.isBlank() ? subject : username
        );
    }

    private String stripBearerPrefix(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Missing bearer token");
        }

        String value = authorizationHeader.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return value.substring(7).trim();
        }

        return value;
    }

    private boolean isAccessTokenRevoked(Claims claims, String userId) {
        String tokenId = claims.getId();
        if (tokenId != null && !tokenId.isBlank()
                && Boolean.TRUE.equals(redisTemplate.hasKey("revoked_access_token:" + tokenId))) {
            return true;
        }

        String validAfter = redisTemplate.opsForValue().get("access_token_valid_after:" + userId);
        if (validAfter == null || validAfter.isBlank()) {
            return false;
        }

        Date issuedAt = claims.getIssuedAt();
        if (issuedAt == null) {
            return false;
        }

        try {
            return issuedAt.getTime() < Long.parseLong(validAfter);
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public record AuthenticatedUser(UUID userId, String username) {}
}
