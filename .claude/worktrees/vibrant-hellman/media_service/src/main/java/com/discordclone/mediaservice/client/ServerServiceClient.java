package com.discordclone.mediaservice.client;

import com.discordclone.common.dto.ApiResponse;
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

    public void checkChannelAccess(Long channelId, UUID userId) {
        if (!canUploadAttachment(channelId, userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have permission to upload to this channel");
        }
    }

    private boolean canUploadAttachment(Long channelId, UUID userId) {
        try {
            ApiResponse<Boolean> response = restClientBuilder.build()
                    .get()
                    .uri(serverServiceBaseUrl + "/api/servers/channels/{channelId}/can-upload-attachment?userId={userId}",
                            channelId,
                            userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<Boolean>>() {
                    });

            return response != null && Boolean.TRUE.equals(response.getData());
        } catch (Exception ex) {
            throw new AppException(HttpStatus.FORBIDDEN, "Cannot verify channel permission");
        }
    }
}
