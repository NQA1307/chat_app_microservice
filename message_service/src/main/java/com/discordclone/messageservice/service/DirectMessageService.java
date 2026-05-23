package com.discordclone.messageservice.service;

import com.discordclone.common.event.DirectMessageSentEvent;
import com.discordclone.common.exception.AppException;
import com.discordclone.messageservice.dto.request.SendDirectMessageRequest;
import com.discordclone.messageservice.dto.request.UpdateMessageRequest;
import com.discordclone.messageservice.dto.response.ConversationResponse;
import com.discordclone.messageservice.dto.response.DirectMessageResponse;
import com.discordclone.messageservice.entity.Conversation;
import com.discordclone.messageservice.entity.DirectMessage;
import com.discordclone.messageservice.repository.ConversationRepository;
import com.discordclone.messageservice.repository.DirectMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DirectMessageService {

    private final ConversationRepository conversationRepository;
    private final DirectMessageRepository directMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageEventPublisher  messageEventPublisher;

    @Transactional
    public ConversationResponse getOrCreateConversation(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot create conversation with yourself");
        }

        UUID id1 = currentUserId.compareTo(targetUserId) <= 0 ? currentUserId : targetUserId;
        UUID id2 = currentUserId.compareTo(targetUserId) <= 0 ? targetUserId : currentUserId;

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

    public List<ConversationResponse> getConversations(UUID userId) {
        return conversationRepository.findByParticipantId1OrParticipantId2(userId, userId)
                .stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Transactional
    public DirectMessageResponse sendDirectMessage(SendDirectMessageRequest req, UUID currentUserId, String username) {
        Conversation conversation = getConversationForParticipant(req.getConversationId(), currentUserId);

        DirectMessage message = DirectMessage.builder()
                .conversationId(conversation.getId())
                .senderId(currentUserId)
                .senderUsername(username)
                .content(req.getContent())
                .build();

        message = directMessageRepository.save(message);

        DirectMessageResponse response = toDirectMessageResponse(message);

        messagingTemplate.convertAndSend(
                "/topic/dm/" + conversation.getId(),
                response
        );

        UUID receiverId = conversation.getParticipantId1().equals(currentUserId)
                ? conversation.getParticipantId2()
                : conversation.getParticipantId1();
        LocalDateTime eventCreatedAt = response.getCreatedAt() != null
                ? response.getCreatedAt()
                : LocalDateTime.now();

        messageEventPublisher.publishDirectMessageSent(
                new DirectMessageSentEvent(
                        response.getId(),
                        response.getConversationId(),
                        response.getSenderId(),
                        receiverId,
                        response.getContent(),
                        eventCreatedAt.atZone(ZoneId.systemDefault()).toInstant()
                )
        );

        return response;
    }

    public List<DirectMessageResponse> getDirectMessages(Long conversationId, UUID currentUserId, int page, int size) {
        getConversationForParticipant(conversationId, currentUserId);

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

    private Conversation getConversationForParticipant(Long conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Conversation not found"));

        boolean isParticipant = conversation.getParticipantId1().equals(userId)
                || conversation.getParticipantId2().equals(userId);
        if (!isParticipant) {
            throw new AppException(HttpStatus.FORBIDDEN, "You are not a participant of this conversation");
        }

        return conversation;
    }

    private DirectMessageResponse toDirectMessageResponse(DirectMessage m) {
        return DirectMessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .senderUsername(m.getSenderUsername())
                .content(m.getContent())
                .fileUrl(m.getFileUrl())
                .deleted(m.isDeleted())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    //Cap nhat tin nhan chi danh cho user la chu nhan tin nhan
    @Transactional
    public DirectMessageResponse updateDirectMessage(
        String messageId,
        UUID currentUserId,
        UpdateMessageRequest req
) {
    DirectMessage message = directMessageRepository.findById(messageId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Message not found"));

    if (message.isDeleted()) {
        throw new AppException(HttpStatus.BAD_REQUEST, "Message has been deleted");
    }

    getConversationForParticipant(message.getConversationId(), currentUserId);

    if (!message.getSenderId().equals(currentUserId)) {
        throw new AppException(HttpStatus.FORBIDDEN, "You can only edit your own message");
    }

    String content = req.getContent() == null ? "" : req.getContent().trim();
    if (content.isBlank()) {
        throw new AppException(HttpStatus.BAD_REQUEST, "Message content cannot be empty");
    }

    message.setContent(content);
    DirectMessage saved = directMessageRepository.save(message);
    DirectMessageResponse response = toDirectMessageResponse(saved);

    messagingTemplate.convertAndSend(
            "/topic/dm/" + saved.getConversationId(),
            response
    );

    return response;
    }


    //Xoa tin nhan DM
    @Transactional
    public DirectMessageResponse deleteDirectMessage(
        String messageId,
        UUID currentUserId
) {
    DirectMessage message = directMessageRepository.findById(messageId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Message not found"));

    if (message.isDeleted()) {
        throw new AppException(HttpStatus.BAD_REQUEST, "Message already deleted");
    }

    getConversationForParticipant(message.getConversationId(), currentUserId);

    if (!message.getSenderId().equals(currentUserId)) {
        throw new AppException(HttpStatus.FORBIDDEN, "You can only delete your own message");
    }

    message.setDeleted(true);
    message.setContent("");

    DirectMessage saved = directMessageRepository.save(message);
    DirectMessageResponse response = toDirectMessageResponse(saved);

    messagingTemplate.convertAndSend(
            "/topic/dm/" + saved.getConversationId(),
            response
    );

    return response;
}



}
