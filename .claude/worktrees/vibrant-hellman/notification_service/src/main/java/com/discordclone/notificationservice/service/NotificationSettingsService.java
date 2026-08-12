package com.discordclone.notificationservice.service;

import com.discordclone.notificationservice.entity.NotificationSettings;
import com.discordclone.notificationservice.repository.NotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final NotificationSettingsRepository repository;

    public NotificationSettings getSettings(UUID userId) {
        return repository.findById(userId)
                .orElseGet(() -> repository.save(NotificationSettings.builder().userId(userId).build()));
    }

    @Transactional
    public Map<UUID, NotificationSettings> getSettingsForUsers(Set<UUID> userIds) {
        Map<UUID, NotificationSettings> existingSettings = repository.findAllById(userIds).stream()
                .collect(Collectors.toMap(NotificationSettings::getUserId, Function.identity()));

        List<NotificationSettings> missingSettings = userIds.stream()
                .filter(userId -> !existingSettings.containsKey(userId))
                .map(userId -> NotificationSettings.builder().userId(userId).build())
                .toList();

        if (!missingSettings.isEmpty()) {
            repository.saveAll(missingSettings)
                    .forEach(settings -> existingSettings.put(settings.getUserId(), settings));
        }

        return existingSettings;
    }

    @Transactional
    public NotificationSettings updateSettings(UUID userId, boolean dm, boolean server, boolean friend) {
        NotificationSettings settings = getSettings(userId);
        settings.setDmEnabled(dm);
        settings.setServerEnabled(server);
        settings.setFriendRequestEnabled(friend);
        return repository.save(settings);
    }
}
