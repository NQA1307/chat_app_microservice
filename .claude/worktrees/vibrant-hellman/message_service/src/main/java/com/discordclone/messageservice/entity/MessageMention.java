package com.discordclone.messageservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "message_mentions")
@Getter
@Setter 
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class MessageMention {
    @Id
    @Column(length = 26, nullable = false, updatable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private ReactionSourceType sourceType;

    @Column(name = "source_id", nullable = false, length = 64)
    private String sourceId;

    @Column(name = "mentioned_user_id")
    private UUID mentionedUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mention_type", nullable = false, length = 30)
    private MentionType mentionType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = com.discordclone.common.util.UlidGenerator.next();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
