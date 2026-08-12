package com.discordclone.voiceservice.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.common.exception.AppException;
import com.discordclone.voiceservice.client.ServerServiceClient;
import com.discordclone.voiceservice.dto.request.SaveVoiceE2eeEnvelopeRequest;
import com.discordclone.voiceservice.dto.response.VoiceE2eeEnvelopeResponse;
import com.discordclone.voiceservice.entity.VoiceE2eeKeyEnvelope;
import com.discordclone.voiceservice.repository.VoiceE2eeKeyEnvelopeRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/voice/channels/{channelId}/e2ee")
@RequiredArgsConstructor
public class VoiceE2eeController {

    private final ServerServiceClient serverServiceClient;
    private final VoiceE2eeKeyEnvelopeRepository envelopeRepository;

    @GetMapping("/envelope")
    public ApiResponse<VoiceE2eeEnvelopeResponse> getEnvelope(
            @PathVariable("channelId") Long channelId,
            @RequestParam("roomName") String roomName,
            @RequestParam("deviceId") String deviceId,
            HttpServletRequest request
    ) {
        UUID userId = currentUserId(request);
        assertCanAccess(channelId, userId);

        VoiceE2eeKeyEnvelope envelope = envelopeRepository
                .findByRoomNameAndRecipientUserIdAndRecipientDeviceIdOrderByKeyVersionDesc(
                        roomName, userId, deviceId
                )
                .stream()
                .filter(item -> item.getRevokedAt() == null)
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "E2EE envelope not found"));

        return ApiResponse.success(new VoiceE2eeEnvelopeResponse(
                envelope.getRoomName(),
                envelope.getChannelId(),
                envelope.getKeyVersion(),
                envelope.getEncryptedKey(),
                envelope.getIv(),
                envelope.getAlgorithm()
        ));
    }

    @PostMapping("/envelopes")
    public ApiResponse<Void> saveEnvelopes(
            @PathVariable("channelId") Long channelId,
            @RequestBody SaveVoiceE2eeEnvelopeRequest body,
            HttpServletRequest request
    ) {
        UUID senderUserId = currentUserId(request);
        assertCanAccess(channelId, senderUserId);

        for (var item : body.envelopes()) {
            envelopeRepository.save(VoiceE2eeKeyEnvelope.builder()
                    .roomName(body.roomName())
                    .channelId(channelId)
                    .senderUserId(senderUserId)
                    .recipientUserId(item.recipientUserId())
                    .recipientDeviceId(item.recipientDeviceId())
                    .keyVersion(body.keyVersion())
                    .encryptedKey(item.encryptedKey())
                    .iv(item.iv())
                    .algorithm(item.algorithm())
                    .build());
        }

        return ApiResponse.success("E2EE envelopes saved", null);
    }

    private void assertCanAccess(Long channelId, UUID userId) {
        if (!serverServiceClient.canJoinVoice(channelId, userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "No access to voice channel");
        }
    }

    private UUID currentUserId(HttpServletRequest request) {
        return UUID.fromString(request.getHeader("X-User-Id"));
    }
}
