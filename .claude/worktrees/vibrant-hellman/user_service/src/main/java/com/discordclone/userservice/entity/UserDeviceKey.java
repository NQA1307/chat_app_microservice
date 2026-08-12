package com.discordclone.userservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_device_keys", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id","device_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeviceKey {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name ="device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "public_key", nullable = true, columnDefinition = "TEXT")
    private String publicKey;

    @Column(nullable = true, length = 50)
    private String algorithm;

    @Column(name = "push_token", length = 255)
    private String pushToken;

    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (algorithm == null) algorithm = "ECDH-P256";
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
