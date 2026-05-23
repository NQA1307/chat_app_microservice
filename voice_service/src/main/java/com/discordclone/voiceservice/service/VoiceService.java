package com.discordclone.voiceservice.service;

import com.discordclone.common.exception.AppException;
import com.discordclone.voiceservice.client.ServerServiceClient;
import com.discordclone.voiceservice.dto.request.JoinVoiceRequest;
import com.discordclone.voiceservice.dto.response.VoiceTokenResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoiceService {

    private final ServerServiceClient serverServiceClient;
    private final LiveKitTokenService liveKitTokenService;

    @Value("${livekit.url}")
    private String livekitUrl;

    @Value("${livekit.max-participants-per-room:10}")
    private int maxParticipantsPerRoom;

    public VoiceTokenResponse createJoinToken(
            Long channelId,
            JoinVoiceRequest request,
            UUID userId,
            String username
    ) {
        if (!serverServiceClient.canAccessChannel(channelId, userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have access to this voice channel");
        }

        String roomName = buildRoomName(request.serverId(), channelId);
        String token = liveKitTokenService.createJoinToken(roomName, userId, username);

        return new VoiceTokenResponse(livekitUrl, token, roomName, maxParticipantsPerRoom);
    }

    private String buildRoomName(Long serverId, Long channelId) {
        return "server-" + serverId + "-channel-" + channelId;
    }
}
