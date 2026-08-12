package com.discordclone.voiceservice.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class LiveKitTokenService {

    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    @Value("${livekit.token-ttl-minutes:60}")
    private long tokenTtlMinutes;

    public String createJoinToken(String roomName, UUID userId, String username) {
        Instant now = Instant.now();

        Map<String, Object> videoGrant = new HashMap<>();
        videoGrant.put("roomJoin", true);
        videoGrant.put("room", roomName);
        videoGrant.put("canPublish", true);
        videoGrant.put("canSubscribe", true);
        videoGrant.put("canPublishData", true);

        return Jwts.builder()
                .setIssuer(apiKey)
                .setSubject(userId.toString())
                .claim("name", username)
                .claim("video", videoGrant)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(tokenTtlMinutes * 60)))
                .signWith(
                        new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"),
                        SignatureAlgorithm.HS256)
                .compact();
    }
}
