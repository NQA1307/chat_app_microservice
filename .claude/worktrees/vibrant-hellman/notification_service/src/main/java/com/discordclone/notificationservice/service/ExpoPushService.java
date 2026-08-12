package com.discordclone.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExpoPushService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    public void sendPushNotification(String expoToken, String title, String body, Map<String, Object> data) {
        if (expoToken == null || !expoToken.startsWith("ExponentPushToken")) {
            return;
        }

        Map<String, Object> payload = Map.of(
            "to", expoToken,
            "title", title,
            "body", body,
            "data", data != null ? data : Map.of()
        );

        try {
            restTemplate.postForEntity(EXPO_PUSH_URL, List.of(payload), String.class);
            log.debug("Push notification sent to Expo token: {}", expoToken);
        } catch (Exception e) {
            log.error("Failed to send push notification to Expo: {}", e.getMessage());
        }
    }
}
