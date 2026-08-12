package com.discordclone.notificationservice.service;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.notificationservice.dto.ExpoPushRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PushNotificationService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final RestTemplate internalRestTemplate;
    private final RestTemplate expoRestTemplate;
    private final String internalServiceSecret;

    public PushNotificationService(
            @Qualifier("internalRestTemplate") RestTemplate internalRestTemplate,
            @Qualifier("expoRestTemplate") RestTemplate expoRestTemplate,
            @Value("${internal.service-secret}") String internalServiceSecret) {
        this.internalRestTemplate = internalRestTemplate;
        this.expoRestTemplate = expoRestTemplate;
        this.internalServiceSecret = internalServiceSecret;
    }

    @Async
    public void sendPush(UUID userId, String title, String body, Map<String, Object> data) {
        List<String> tokens = getPushTokens(userId);
        if (tokens.isEmpty()) {
            log.debug("No push tokens found for user {}", userId);
            return;
        }

        for (String token : tokens) {
            if (!isExpoPushToken(token)) {
                log.debug("Skipping invalid Expo push token for user {}", userId);
                continue;
            }

            ExpoPushRequest request = ExpoPushRequest.builder()
                    .to(token)
                    .title(title)
                    .body(body)
                    .sound("default")
                    .data(data)
                    .build();

            sendToExpo(userId, token, request);
        }
    }

    private List<String> getPushTokens(UUID userId) {
        try {
            String url = "http://user-service/internal/users/" + userId + "/push-tokens";
            ApiResponse<List<String>> response = internalRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(internalHeaders()),
                    new ParameterizedTypeReference<ApiResponse<List<String>>>() {}
            ).getBody();

            return response != null && response.getData() != null ? response.getData() : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch push tokens for user {}", userId, e);
            return List.of();
        }
    }

    private void sendToExpo(UUID userId, String token, ExpoPushRequest request) {
        try {
            ResponseEntity<Map> response = expoRestTemplate.postForEntity(EXPO_PUSH_URL, request, Map.class);
            Object data = response.getBody() == null ? null : response.getBody().get("data");

            if (data instanceof Map<?, ?> responseData) {
                Object status = responseData.get("status");
                if ("error".equals(status)) {
                    Object message = responseData.get("message");
                    log.warn("Expo rejected push for user {}: {}", userId, message);

                    if (isDeviceNotRegistered(responseData)) {
                        revokePushToken(token);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send Expo push to user {}", userId, e);
        }
    }

    private void revokePushToken(String token) {
        try {
            internalRestTemplate.exchange(
                    "http://user-service/internal/users/push-tokens/revoke",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("pushToken", token), internalHeaders()),
                    new ParameterizedTypeReference<ApiResponse<Void>>() {}
            );
        } catch (Exception e) {
            log.warn("Failed to revoke invalid push token", e);
        }
    }

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", internalServiceSecret);
        return headers;
    }

    private boolean isExpoPushToken(String token) {
        return token != null
                && (token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken["));
    }

    private boolean isDeviceNotRegistered(Map<?, ?> responseData) {
        Object details = responseData.get("details");
        if (details instanceof Map<?, ?> detailsMap) {
            return "DeviceNotRegistered".equals(detailsMap.get("error"));
        }
        return false;
    }
}
