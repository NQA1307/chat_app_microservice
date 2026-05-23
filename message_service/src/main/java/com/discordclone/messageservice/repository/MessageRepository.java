package com.discordclone.messageservice.repository;

import com.discordclone.messageservice.entity.Message;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    // Lấy tin nhắn theo channelId, sắp xếp mới nhất trước, có phân trang
    Page<Message> findByChannelIdOrderByCreatedAtDesc(Long channelId, Pageable pageable);

    
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
