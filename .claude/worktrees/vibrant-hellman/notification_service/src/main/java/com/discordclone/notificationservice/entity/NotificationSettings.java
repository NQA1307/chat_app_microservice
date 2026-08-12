package com.discordclone.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "notification_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettings {

    @Id
    private UUID userId;

    @Column(nullable = false)
    @Builder.Default
    private boolean dmEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean serverEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean friendRequestEnabled = true;

    @Column
    private String expoPushToken;
}
