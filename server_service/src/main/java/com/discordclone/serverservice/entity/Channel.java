package com.discordclone.serverservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "channels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ChannelType type = ChannelType.TEXT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    @ToString.Exclude
    private Server server;

    public enum ChannelType {
        TEXT, VOICE
    }
}
