package com.discordclone.messageservice.repository;

import com.discordclone.messageservice.entity.DirectMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    Page<DirectMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);
}
