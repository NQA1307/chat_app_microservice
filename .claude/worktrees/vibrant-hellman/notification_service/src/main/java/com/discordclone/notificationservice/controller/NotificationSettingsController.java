package com.discordclone.notificationservice.controller;

import com.discordclone.notificationservice.entity.NotificationSettings;
import com.discordclone.notificationservice.service.NotificationSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/settings")
@RequiredArgsConstructor
public class NotificationSettingsController {

    private final NotificationSettingsService settingsService;

    @GetMapping
    public NotificationSettings getSettings(@RequestHeader("X-User-Id") UUID userId) {
        return settingsService.getSettings(userId);
    }

    @PutMapping
    public NotificationSettings updateSettings(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam boolean dmEnabled,
            @RequestParam boolean serverEnabled,
            @RequestParam boolean friendRequestEnabled) {
        return settingsService.updateSettings(userId, dmEnabled, serverEnabled, friendRequestEnabled);
    }
}
