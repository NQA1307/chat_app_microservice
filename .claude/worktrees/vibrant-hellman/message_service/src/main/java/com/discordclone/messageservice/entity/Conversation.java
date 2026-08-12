package com.discordclone.messageservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"participant_id1", "participant_id2"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "participant_id1", nullable = false)
    private UUID participantId1;

    @Column(name = "participant_id2", nullable = false)
    private UUID participantId2;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
