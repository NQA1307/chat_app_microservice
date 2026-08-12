package com.discordclone.voiceservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "voice_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSession {
    @Id
    private UUID id;

    @Column(name = "room_name", nullable = false)
    private String roomName;

    @Column(name = "server_id", nullable = false)
    private Long serverId;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "started_by", nullable = false)
    private UUID startedBy;

    @Column(name = "e2ee_key_owner_user_id")
    private UUID e2eeKeyOwnerUserId;

    @Builder.Default
    @Column(name = "current_key_version", nullable = false)
    private Integer currentKeyVersion = 0;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (startedAt == null) startedAt = LocalDateTime.now();
    }
}
