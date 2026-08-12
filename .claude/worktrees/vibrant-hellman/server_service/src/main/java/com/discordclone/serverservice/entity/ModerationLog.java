package com.discordclone.serverservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.discordclone.common.enums.ModerationAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
    @Table(name = "moderation_logs")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class ModerationLog {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "server_id", nullable = false)
        private Long serverId;

        @Column(name = "actor_id", nullable = false)
        private UUID actorId;

        @Column(name = "target_user_id", nullable = false)
        private UUID targetUserId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private ModerationAction action;

        @Column(length = 500)
        private String reason;

        @CreationTimestamp
        @Column(nullable = false, updatable = false)
        private LocalDateTime createdAt;
    }
