package com.discordclone.voiceservice.client;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServerServiceClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${clients.server-service.base-url:http://localhost:8082}")
    private String serverServiceBaseUrl;

    public boolean canAccessChannel(Long channelId, UUID userId) {
        try {
            ApiResponse<Boolean> response = restClientBuilder.build()
                    .get()
                    .uri(serverServiceBaseUrl + "/api/servers/channels/{channelId}/access?userId={userId}",
                            channelId,
                            userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<Boolean>>() {
                    });

            return response != null && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            logServerServiceFailure("voice channel access", channelId, userId, e);
            throw new AppException(HttpStatus.FORBIDDEN, "Cannot verify voice channel access");
        }
    }

    public boolean canJoinVoice(Long channelId, UUID userId) {
        try {
            ApiResponse<Boolean> response = restClientBuilder.build()
                    .get()
                    .uri(serverServiceBaseUrl + "/api/servers/channels/{channelId}/can-join-voice?userId={userId}",
                            channelId,
                            userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<Boolean>>() {
                    });

            return response != null && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            logServerServiceFailure("join-voice", channelId, userId, e);
            throw new AppException(HttpStatus.FORBIDDEN, "Cannot verify voice channel access");
        }
    }

    private void logServerServiceFailure(String operation, Long channelId, UUID userId, Exception e) {
        if (e instanceof RestClientResponseException responseException) {
            log.warn(
                    "Failed to verify {} via server-service. channelId={}, userId={}, status={}, body={}",
                    operation,
                    channelId,
                    userId,
                    responseException.getStatusCode(),
                    responseException.getResponseBodyAsString()
            );
            return;
        }

        log.warn(
                "Failed to verify {} via server-service. channelId={}, userId={}, error={}",
                operation,
                channelId,
                userId,
                e.getMessage(),
                e
        );
    }
}
