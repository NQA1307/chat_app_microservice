package com.discordclone.voiceservice.repository;

import java.util.List;
import java.util.UUID;

import com.discordclone.voiceservice.entity.VoiceE2eeKeyEnvelope;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoiceE2eeKeyEnvelopeRepository extends JpaRepository<VoiceE2eeKeyEnvelope, UUID> {
    List<VoiceE2eeKeyEnvelope> findByRoomNameAndRecipientUserIdAndRecipientDeviceIdOrderByKeyVersionDesc(
            String roomName,
            UUID recipientUserId,
            String recipientDeviceId
    );

    List<VoiceE2eeKeyEnvelope> findByRoomNameAndKeyVersion(String roomName, Integer keyVersion);
}
