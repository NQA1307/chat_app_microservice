package com.discordclone.messageservice.repository;

import com.discordclone.messageservice.entity.DirectMessage;

import de.huxhorn.sulky.ulid.ULID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, String> {

    Page<DirectMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);
}
