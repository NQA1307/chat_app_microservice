package com.discordclone.mediaservice.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "media_assets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAsset {
    @Id
    private String id;

    @Column(nullable = false)
    private String checkSum;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private String storageProvider;

    @Column(nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private String publicUrl;

    @Column(nullable = false)
    private String resourceType;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
