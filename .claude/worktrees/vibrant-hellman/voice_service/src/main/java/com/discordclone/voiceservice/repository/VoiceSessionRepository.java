package com.discordclone.voiceservice.repository;

import java.util.Optional;
import java.util.UUID;

import com.discordclone.voiceservice.entity.VoiceSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoiceSessionRepository extends JpaRepository<VoiceSession, UUID> {
    Optional<VoiceSession> findByRoomNameAndEndedAtIsNull(String roomName);
    Optional<VoiceSession> findByChannelIdAndEndedAtIsNull(Long channelId);
}
