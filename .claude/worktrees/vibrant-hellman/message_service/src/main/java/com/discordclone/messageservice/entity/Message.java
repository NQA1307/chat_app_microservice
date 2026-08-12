package com.discordclone.messageservice.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.discordclone.common.util.UlidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @Column(length = 26, nullable = false, updatable = false)
    private String id;

    @PrePersist
    void prePersist() {
        if (id == null)
            id = UlidGenerator.next();
    }

    @Column(nullable = false)
    private Long channelId;

    @Column(nullable = false)
    private UUID senderId;

    @Column(nullable = false)
    private String senderUsername;

    @Convert(converter = com.discordclone.messageservice.security.CryptoConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "reply_to_message_id")
    private String replyToMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
