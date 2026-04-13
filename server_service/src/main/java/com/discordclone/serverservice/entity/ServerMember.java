package com.discordclone.serverservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "server_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"server_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    @ToString.Exclude
    private Server server;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    @CreationTimestamp
    private LocalDateTime joinedAt;

    public enum MemberRole {
        OWNER, ADMIN, MEMBER
    }
}
