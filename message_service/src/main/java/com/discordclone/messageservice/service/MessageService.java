package com.discordclone.messageservice.service;

import com.discordclone.messageservice.dto.MessageResponse;
import com.discordclone.messageservice.dto.SendMessageRequest;
import com.discordclone.messageservice.entity.Message;
import com.discordclone.messageservice.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate; // dùng để broadcast WebSocket

    // ── Gửi tin nhắn (lưu DB + broadcast WebSocket) ───────────
    @Transactional
    public MessageResponse sendMessage(SendMessageRequest req) {
        Message message = Message.builder()
                .channelId(req.getChannelId())
                .senderId(req.getSenderId())
                .senderUsername(req.getSenderUsername())
                .content(req.getContent())
                .build();

        message = messageRepository.save(message);
        MessageResponse response = toResponse(message);

        // Broadcast tới tất cả client đang subscribe /topic/channel/{channelId}
        messagingTemplate.convertAndSend(
                "/topic/channel/" + req.getChannelId(),
                response
        );

        return response;
    }

    // ── Lấy lịch sử tin nhắn (có phân trang) ─────────────────
    public List<MessageResponse> getMessages(Long channelId, int page, int size) {
        Page<Message> messages = messageRepository
                .findByChannelIdOrderByCreatedAtDesc(channelId, PageRequest.of(page, size));
        return messages.getContent().stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Mapper ─────────────────────────────────────────────────
    private MessageResponse toResponse(Message m) {
        return MessageResponse.builder()
                .id(m.getId())
                .channelId(m.getChannelId())
                .senderId(m.getSenderId())
                .senderUsername(m.getSenderUsername())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
