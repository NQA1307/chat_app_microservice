package com.discordclone.messageservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.messageservice.dto.request.PresenceBatchRequest;
import com.discordclone.messageservice.dto.response.PresenceStatusResponse;
import com.discordclone.messageservice.service.PresenceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/users/{userId}")
    public ApiResponse<PresenceStatusResponse> getUserPresence(
        @PathVariable("userId") UUID userId
    ) {
        return ApiResponse.success(presenceService.getStatus(userId));
    }

    @PostMapping("/batch")
    public ApiResponse<List<PresenceStatusResponse>> getPresenceBatch(
        @Valid @RequestBody PresenceBatchRequest request
    ) {
        return ApiResponse.success(presenceService.getStatuses(request.getUserIds()));
    }
    
}
