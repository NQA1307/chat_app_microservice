package com.discordclone.messageservice.repository;

import com.discordclone.messageservice.entity.DirectMessage;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, String> {

    Page<DirectMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    Page<DirectMessage> findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    @Query("""
            select m from DirectMessage m
            where m.conversationId = :conversationId
              and m.deleted = false
              and (:beforeId is null or m.id < :beforeId)
            order by m.id desc
            """)
    List<DirectMessage> findConversationMessagesBefore(
            @Param("conversationId") Long conversationId,
            @Param("beforeId") String beforeId,
            Pageable pageable
    );

    @Query("""
            select m from DirectMessage m
            where m.conversationId = :conversationId
              and m.deleted = false
              and (
                lower(coalesce(m.content, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(m.senderUsername, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(m.fileName, '')) like lower(concat('%', :query, '%'))
              )
            order by m.createdAt desc
            """)
    Page<DirectMessage> searchDirectMessages(
            @Param("conversationId") Long conversationId,
            @Param("query") String query,
            Pageable pageable
    );
}
