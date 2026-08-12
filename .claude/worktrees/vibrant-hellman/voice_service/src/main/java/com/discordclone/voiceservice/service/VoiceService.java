package com.discordclone.voiceservice.service;

import com.discordclone.common.exception.AppException;
import com.discordclone.voiceservice.client.ServerServiceClient;
import com.discordclone.voiceservice.dto.request.JoinVoiceRequest;
import com.discordclone.voiceservice.dto.response.VoiceTokenResponse;
import com.discordclone.voiceservice.entity.VoiceSession;
import com.discordclone.voiceservice.entity.VoiceSessionParticipant;
import com.discordclone.voiceservice.repository.VoiceSessionParticipantRepository;
import com.discordclone.voiceservice.repository.VoiceSessionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoiceService {

    private final ServerServiceClient serverServiceClient;
    private final LiveKitTokenService liveKitTokenService;
    private final VoiceSessionRepository voiceSessionRepository;
    private final VoiceSessionParticipantRepository participantRepository;

    @Value("${livekit.url}")
    private String livekitUrl;

    @Value("${livekit.max-participants-per-room:10}")
    private int maxParticipantsPerRoom;

    @Transactional
    public VoiceTokenResponse createJoinToken(
            Long channelId,
            JoinVoiceRequest request,
            UUID userId,
            String username
    ) {
        if (!serverServiceClient.canJoinVoice(channelId, userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have access to this voice channel");
        }

        String roomName = buildRoomName(request.serverId(), channelId);

        VoiceSession session = voiceSessionRepository
                .findByRoomNameAndEndedAtIsNull(roomName)
                .orElseGet(() -> voiceSessionRepository.save(
                        VoiceSession.builder()
                                .roomName(roomName)
                                .serverId(request.serverId())
                                .channelId(channelId)
                                .startedBy(userId)
                                .e2eeKeyOwnerUserId(userId)
                                .currentKeyVersion(1)
                                .build()
                ));

        participantRepository
                .findBySessionIdAndUserIdAndLeftAtIsNull(session.getId(), userId)
                .orElseGet(() -> participantRepository.save(
                        VoiceSessionParticipant.builder()
                                .sessionId(session.getId())
                                .userId(userId)
                                .build()
                ));

        String token = liveKitTokenService.createJoinToken(roomName, userId, username);
        return new VoiceTokenResponse(livekitUrl, token, roomName, maxParticipantsPerRoom, session.getE2eeKeyOwnerUserId(),session.getCurrentKeyVersion());
    }

    private String buildRoomName(Long serverId, Long channelId) {
        return "server-" + serverId + "-channel-" + channelId;
    }
}
