package com.discordclone.notificationservice.repository;

import com.discordclone.notificationservice.entity.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, UUID> {
}
