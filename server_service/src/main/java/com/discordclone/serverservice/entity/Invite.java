package com.discordclone.serverservice.entity;

import com.discordclone.common.enums.InviteStatus;
import com.discordclone.common.enums.InviteType;
import com.discordclone.common.util.UlidGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;



@Entity
@Table(name = "invites")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invite {
    @Id
    @Column(length = 26, nullable = false, updatable = false)
    private String id;

    @PrePersist
    public void prePersist(){
        if (id == null)
            id = UlidGenerator.next();
    }

    @Column(nullable = false)
    private UUID senderId;

    @Column(nullable = false)
    private UUID receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InviteType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InviteStatus status = InviteStatus.PENDING;

    @Column(nullable = false)
    private String targetType;

    @Column(nullable = false)
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime expiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
