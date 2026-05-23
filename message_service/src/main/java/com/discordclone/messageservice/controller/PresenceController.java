package com.discordclone.messageservice.controller;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.messageservice.dto.response.PresenceStatusResponse;
import com.discordclone.messageservice.service.PresenceService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/users/{userId}")
    public ApiResponse<PresenceStatusResponse> getUserPresence(
        @PathVariable UUID userId
    ) {
        return ApiResponse.success(presenceService.getStatus(userId));
    }
    
}
