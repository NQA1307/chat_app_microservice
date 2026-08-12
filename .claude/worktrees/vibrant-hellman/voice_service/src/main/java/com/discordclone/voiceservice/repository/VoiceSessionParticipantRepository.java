package com.discordclone.voiceservice.repository;

import java.util.Optional;
import java.util.UUID;

import com.discordclone.voiceservice.entity.VoiceSessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoiceSessionParticipantRepository extends JpaRepository<VoiceSessionParticipant, UUID> {
    Optional<VoiceSessionParticipant> findBySessionIdAndUserIdAndLeftAtIsNull(UUID sessionId, UUID userId);
}
