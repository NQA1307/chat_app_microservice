package com.discordclone.messageservice.repository;

import com.discordclone.messageservice.entity.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    // Lấy tin nhắn theo channelId, sắp xếp mới nhất trước, có phân trang
    Page<Message> findByChannelIdOrderByCreatedAtDesc(Long channelId, Pageable pageable);

    Page<Message> findByChannelIdAndDeletedFalseOrderByCreatedAtDesc(Long channelId, Pageable pageable);

    @Query("""
            select m from Message m
            where m.channelId = :channelId
              and m.deleted = false
              and (:beforeId is null or m.id < :beforeId)
            order by m.id desc
            """)
    List<Message> findChannelMessagesBefore(
            @Param("channelId") Long channelId,
            @Param("beforeId") String beforeId,
            Pageable pageable
    );

    @Query("""
            select m from Message m
            where m.channelId = :channelId
              and m.deleted = false
              and (
                lower(coalesce(m.content, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(m.senderUsername, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(m.fileName, '')) like lower(concat('%', :query, '%'))
              )
            order by m.createdAt desc
            """)
    Page<Message> searchChannelMessages(
            @Param("channelId") Long channelId,
            @Param("query") String query,
            Pageable pageable
    );

    
    long countByChannelIdAndIdGreaterThanAndSenderIdNotAndDeletedFalse(
        Long channelId,
        String messageId,
        UUID senderId
    );
    //dem so tin nhan trong channel dua tren channelId va id nguoi gui
    long countByChannelIdAndSenderIdNotAndDeletedFalse(
        Long channelId,
        UUID senderId
    );

    Optional<Message> findTopByChannelIdAndDeletedFalseOrderByIdDesc(Long channelId);
}
