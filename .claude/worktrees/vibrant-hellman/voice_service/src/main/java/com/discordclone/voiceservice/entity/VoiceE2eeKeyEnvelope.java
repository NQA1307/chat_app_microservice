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
@Table(name = "voice_e2ee_key_envelopes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceE2eeKeyEnvelope {
    @Id
    private UUID id;

    @Column(name = "room_name", nullable = false)
    private String roomName;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "sender_user_id", nullable = false)
    private UUID senderUserId;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Column(name = "recipient_device_id", nullable = false)
    private String recipientDeviceId;

    @Column(name = "key_version", nullable = false)
    private Integer keyVersion;

    @Column(name = "encrypted_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedKey;

    @Column(nullable = false)
    private String iv;

    @Column(nullable = false)
    private String algorithm;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (algorithm == null) algorithm = "ECDH-P256-AES-GCM";
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
