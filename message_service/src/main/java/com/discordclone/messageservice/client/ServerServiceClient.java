package com.discordclone.messageservice.client;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.common.enums.PermissionCode;
import com.discordclone.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServerServiceClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${clients.server-service.base-url:http://localhost:8082}")
    private String serverServiceBaseUrl;

    public boolean canAccessChannel(Long channelId, UUID userId) {
        try {
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
        } catch (Exception e) {
            throw new AppException(HttpStatus.FORBIDDEN, "Cannot verify channel permission");
        }
    }

    public boolean hasChannelPermission(Long channelId, UUID userId, PermissionCode permission) {
        try {
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
        } catch (Exception e) {
            throw new AppException(HttpStatus.FORBIDDEN, "Cannot verify channel permission");
        }
    }
}
