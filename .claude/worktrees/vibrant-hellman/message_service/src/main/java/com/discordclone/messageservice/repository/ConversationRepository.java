package com.discordclone.messageservice.repository;

import com.discordclone.messageservice.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByParticipantId1AndParticipantId2(UUID participantId1, UUID participantId2);

    List<Conversation> findByParticipantId1OrParticipantId2(UUID participantId1, UUID participantId2);
}
