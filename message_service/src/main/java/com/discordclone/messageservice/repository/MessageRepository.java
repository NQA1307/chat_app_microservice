package com.discordclone.messageservice.repository;

import com.discordclone.messageservice.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Lấy tin nhắn theo channelId, sắp xếp mới nhất trước, có phân trang
    Page<Message> findByChannelIdOrderByCreatedAtDesc(Long channelId, Pageable pageable);
}
