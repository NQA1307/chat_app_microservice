package com.discordclone.messageservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import com.discordclone.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${clients.user-service.base-url:http://localhost:8081}")
    private String userServiceBaseUrl;

    @CircuitBreaker(name = "userService", fallbackMethod = "blockCheckFallback")
    @Retry(name = "userService")
    public BlockStatusResponse checkBlockStatus(UUID userId, UUID targetUserId) {
        ApiResponse<BlockStatusResponse> response = restClientBuilder.build()
                .get()
                .uri(userServiceBaseUrl + "/internal/users/{userId}/blocks/{targetUserId}",
                        userId,
                        targetUserId)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<BlockStatusResponse>>() {});
        return response != null && response.getData() != null ? response.getData() : new BlockStatusResponse(true, "BOTH");
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "filterBlockedPresenceFallback")
    @Retry(name = "userService")
    public List<UUID> filterBlockedPresence(UUID userId, List<UUID> targetUserIds) {
        ApiResponse<List<UUID>> response = restClientBuilder.build()
                .post()
                .uri(userServiceBaseUrl + "/internal/users/{userId}/blocked-relationship/filter", userId)
                .body(targetUserIds)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<List<UUID>>>() {});
        return response != null && response.getData() != null ? response.getData() : Collections.emptyList();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getFriendIdsFallback")
    @Retry(name = "userService")
    public List<UUID> getFriendIds(UUID userId) {
        ApiResponse<List<UUID>> response = restClientBuilder.build()
                .get()
                .uri(userServiceBaseUrl + "/internal/users/{userId}/friends/ids", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<List<UUID>>>() {});
        return response != null && response.getData() != null ? response.getData() : Collections.emptyList();
    }

    // Fallbacks
    public BlockStatusResponse blockCheckFallback(UUID userId, UUID targetUserId, Throwable e) {
        log.warn("UserService checkBlockStatus failed, applying fail-closed fallback. userId={}, targetUserId={}, error={}", 
                userId, targetUserId, e.getMessage());
        return new BlockStatusResponse(true, "BOTH");
    }

    public List<UUID> filterBlockedPresenceFallback(UUID userId, List<UUID> targetUserIds, Throwable e) {
        log.warn("UserService filterBlockedPresence failed, returning empty list. userId={}, error={}", userId, e.getMessage());
        return Collections.emptyList();
    }

    public List<UUID> getFriendIdsFallback(UUID userId, Throwable e) {
        log.warn("UserService getFriendIds failed, returning empty list. userId={}, error={}", userId, e.getMessage());
        return Collections.emptyList();
    }

    public record BlockStatusResponse(boolean blocked, String blockedBy) {}
}
