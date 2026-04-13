package com.discordclone.messageservice.repository;

import com.discordclone.messageservice.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByParticipantId1AndParticipantId2(Long participantId1, Long participantId2);

    List<Conversation> findByParticipantId1OrParticipantId2(Long participantId1, Long participantId2);
}
