package com.discordclone.serverservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "servers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    private String imageUrl;

    @Column(nullable = false)
    private Long ownerId;   // userId từ user-service, không dùng FK chéo service

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<Channel> channels = new ArrayList<>();

    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<ServerMember> members = new ArrayList<>();
}
