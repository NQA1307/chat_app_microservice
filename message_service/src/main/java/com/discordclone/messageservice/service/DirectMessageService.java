package com.discordclone.messageservice.service;

import com.discordclone.messageservice.dto.ConversationResponse;
import com.discordclone.messageservice.dto.DirectMessageResponse;
import com.discordclone.messageservice.dto.SendDirectMessageRequest;
import com.discordclone.messageservice.entity.Conversation;
import com.discordclone.messageservice.entity.DirectMessage;
import com.discordclone.messageservice.repository.ConversationRepository;
import com.discordclone.messageservice.repository.DirectMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectMessageService {

    private final ConversationRepository conversationRepository;
    private final DirectMessageRepository directMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ConversationResponse getOrCreateConversation(Long currentUserId, Long targetUserId) {
        // Luôn lưu id nhỏ hơn vào participantId1 để đảm bảo unique pair
        Long id1 = Math.min(currentUserId, targetUserId);
        Long id2 = Math.max(currentUserId, targetUserId);

        Conversation conversation = conversationRepository
                .findByParticipantId1AndParticipantId2(id1, id2)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder()
                                .participantId1(id1)
                                .participantId2(id2)
                                .build()
                ));

        return toConversationResponse(conversation);
    }

    public List<ConversationResponse> getConversations(Long userId) {
        return conversationRepository.findByParticipantId1OrParticipantId2(userId, userId)
                .stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Transactional
    public DirectMessageResponse sendDirectMessage(SendDirectMessageRequest req) {
        DirectMessage message = DirectMessage.builder()
                .conversationId(req.getConversationId())
                .senderId(req.getSenderId())
                .senderUsername(req.getSenderUsername())
                .content(req.getContent())
                .build();

        message = directMessageRepository.save(message);
        DirectMessageResponse response = toDirectMessageResponse(message);

        // Broadcast tới tất cả client đang subscribe /topic/dm/{conversationId}
        messagingTemplate.convertAndSend(
                "/topic/dm/" + req.getConversationId(),
                response
        );

        return response;
    }

    public List<DirectMessageResponse> getDirectMessages(Long conversationId, int page, int size) {
        return directMessageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(this::toDirectMessageResponse)
                .toList();
    }

    private ConversationResponse toConversationResponse(Conversation c) {
        return ConversationResponse.builder()
                .id(c.getId())
                .participantId1(c.getParticipantId1())
                .participantId2(c.getParticipantId2())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private DirectMessageResponse toDirectMessageResponse(DirectMessage m) {
        return DirectMessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .senderUsername(m.getSenderUsername())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
