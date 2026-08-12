package com.discordclone.voiceservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.common.exception.AppException;
import com.discordclone.voiceservice.dto.request.JoinVoiceRequest;
import com.discordclone.voiceservice.dto.response.VoiceTokenResponse;
import com.discordclone.voiceservice.service.VoiceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceService voiceService;

    @PostMapping("/channels/{channelId}/token")
    public ResponseEntity<ApiResponse<VoiceTokenResponse>> createJoinToken(
            @PathVariable("channelId") Long channelId,
            @Valid @RequestBody JoinVoiceRequest request,
            HttpServletRequest httpRequest
    ) {
        VoiceTokenResponse token = voiceService.createJoinToken(
                channelId,
                request,
                getUserId(httpRequest),
                getUsername(httpRequest)
        );

        return ResponseEntity.ok(ApiResponse.success("Voice token created", token));
    }

    private UUID getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Missing authenticated user headers");
        }
        return UUID.fromString(userId);
    }

    private String getUsername(HttpServletRequest request) {
        String username = request.getHeader("X-User-Username");
        if (username == null || username.isBlank()) {
            return getUserId(request).toString();
        }
        return username;
    }
}
