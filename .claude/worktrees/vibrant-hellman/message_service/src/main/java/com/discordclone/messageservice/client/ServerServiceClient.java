package com.discordclone.messageservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import com.discordclone.common.dto.ApiResponse;
import com.discordclone.common.enums.PermissionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServerServiceClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${clients.server-service.base-url:http://localhost:8082}")
    private String serverServiceBaseUrl;

    @CircuitBreaker(name = "serverService", fallbackMethod = "fallbackCanAccessChannel")
    @Retry(name = "serverService")
    public boolean canAccessChannel(Long channelId, UUID userId) {
        // Message service does not own server membership data, so it asks server-service.
        ApiResponse<Boolean> response = restClientBuilder.build()
                .get()
                .uri(serverServiceBaseUrl + "/api/servers/channels/{channelId}/access?userId={userId}",
                        channelId,
                        userId)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<Boolean>>() {
                });

        return response != null && Boolean.TRUE.equals(response.getData());
    }

    @CircuitBreaker(name = "serverService", fallbackMethod = "fallbackCanSendMessage")
    @Retry(name = "serverService")
    public boolean canSendMessage(Long channelId, UUID userId) {
        ApiResponse<Boolean> response = restClientBuilder.build()
                .get()
                .uri(serverServiceBaseUrl + "/api/servers/channels/{channelId}/can-send-message?userId={userId}",
                        channelId,
                        userId)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<Boolean>>() {
                });

        return response != null && Boolean.TRUE.equals(response.getData());
    }

    @CircuitBreaker(name = "serverService", fallbackMethod = "fallbackHasChannelPermission")
    @Retry(name = "serverService")
    public boolean hasChannelPermission(Long channelId, UUID userId, PermissionCode permission) {
        ApiResponse<Boolean> response = restClientBuilder.build()
                .get()
                .uri(serverServiceBaseUrl + "/api/servers/channels/{channelId}/permissions/{permission}?userId={userId}",
                        channelId,
                        permission.name(),
                        userId)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<Boolean>>() {
                });

        return response != null && Boolean.TRUE.equals(response.getData());
    }

    public boolean fallbackCanAccessChannel(Long channelId, UUID userId, Throwable e) {
        logServerServiceFailure("channel access", channelId, userId, e);
        return false;
    }

    public boolean fallbackCanSendMessage(Long channelId, UUID userId, Throwable e) {
        logServerServiceFailure("send-message", channelId, userId, e);
        return false;
    }

    public boolean fallbackHasChannelPermission(Long channelId, UUID userId, PermissionCode permission, Throwable e) {
        logServerServiceFailure("channel permission " + permission, channelId, userId, e);
        return false;
    }

    private void logServerServiceFailure(String operation, Long channelId, UUID userId, Throwable e) {
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

    @CircuitBreaker(name = "serverService", fallbackMethod = "fallbackGetCoMemberIds")
    @Retry(name = "serverService")
    public List<UUID> getCoMemberIds(UUID userId) {
        ApiResponse<List<UUID>> response = restClientBuilder.build()
                .get()
                .uri(serverServiceBaseUrl + "/internal/servers/users/{userId}/co-member-ids", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<List<UUID>>>() {});
        return response != null && response.getData() != null ? response.getData() : java.util.Collections.emptyList();
    }

    public List<UUID> fallbackGetCoMemberIds(UUID userId, Throwable e) {
        log.warn("ServerService getCoMemberIds failed, returning empty list. userId={}, error={}", userId, e.getMessage());
        return java.util.Collections.emptyList();
    }

    @CircuitBreaker(name = "serverService", fallbackMethod = "fallbackGetChannelMemberIds")
    @Retry(name = "serverService")
    public List<UUID> getChannelMemberIds(Long channelId) {
        ApiResponse<List<UUID>> response = restClientBuilder.build()
                .get()
                .uri(serverServiceBaseUrl + "/internal/servers/channels/{channelId}/member-ids", channelId)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<List<UUID>>>() {});
        return response != null && response.getData() != null
                ? response.getData()
                : java.util.Collections.emptyList();
    }

    public List<UUID> fallbackGetChannelMemberIds(Long channelId, Throwable e) {
        log.warn("ServerService getChannelMemberIds failed. channelId={}, error={}", channelId, e.getMessage());
        return java.util.Collections.emptyList();
    }
}
