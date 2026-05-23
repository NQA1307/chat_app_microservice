package com.discordclone.messageservice.service;

import com.discordclone.common.exception.AppException;
import com.discordclone.messageservice.dto.request.SendMessageRequest;
import com.discordclone.messageservice.dto.request.UpdateMessageRequest;
import com.discordclone.messageservice.dto.response.ChannelUnreadStateResponse;
import com.discordclone.messageservice.dto.response.MessageResponse;
import com.discordclone.messageservice.entity.Message;
import com.discordclone.messageservice.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.discordclone.messageservice.entity.ChannelReadState;
import com.discordclone.messageservice.repository.ChannelReadStateRepository;
import java.time.LocalDateTime;
import java.util.Optional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PermissionService permissionService;
    private final MessageEventPublisher messageEventPublisher;
    private final ChannelReadStateRepository channelReadStateRepository;

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest req) {
        if (req.getSenderId() == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Missing sender id");
        }

        if (req.getSenderUsername() == null || req.getSenderUsername().isBlank()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Missing sender username");
        }

        String content = req.getContent() == null ? "" : req.getContent().trim();
        if (content.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Message content cannot be empty");
        }

        // Validate membership before persisting or broadcasting a channel message.
        if (!permissionService.canAccessChannel(req.getChannelId(), req.getSenderId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have access to this channel");
        }

        Message message = Message.builder()
                .channelId(req.getChannelId())
                .senderId(req.getSenderId())
                .senderUsername(req.getSenderUsername())
                .content(content)
                .build();

        message = messageRepository.save(message);
        MessageResponse response = toResponse(message);

        messagingTemplate.convertAndSend("/topic/channel/" + req.getChannelId(), response);

        messageEventPublisher.publishMessageSent(response);

        return response;
    }

    public List<MessageResponse> getMessages(Long channelId, UUID userId, int page, int size) {
        // History must be protected too, not only send-message.
        if (!permissionService.canAccessChannel(channelId, userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have access to this channel");
        }

        Page<Message> messages = messageRepository
                .findByChannelIdOrderByCreatedAtDesc(channelId, PageRequest.of(page, size));

        return messages.getContent().stream()
                .map(this::toResponse)
                .toList();
    }

    private MessageResponse toResponse(Message m) {
        return MessageResponse.builder()
                .id(m.getId())
                .channelId(m.getChannelId())
                .senderId(m.getSenderId())
                .senderUsername(m.getSenderUsername())
                .content(m.getContent())
                .deleted(m.isDeleted())
                .createdAt(m.getCreatedAt())
                .build();
    }

    //Logic Update tin nhan
    @Transactional
    public MessageResponse updateMessage(String messageId, UUID userId, UpdateMessageRequest req) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Message not found"));

        if (message.isDeleted()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Message has been deleted");
        }

        if (!permissionService.canAccessChannel(message.getChannelId(), userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have access to this channel");
        }

        if (!message.getSenderId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You can only edit your own message");
        }

        String content = req.getContent() == null ? "" : req.getContent().trim();
        if (content.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Message content cannot be empty");
        }

        message.setContent(content);
        Message saved = messageRepository.save(message);

        MessageResponse response = toResponse(saved);

        messagingTemplate.convertAndSend(
                "/topic/channel/" + saved.getChannelId(),
                response);

        return response;
    }

    //Xoa tin nhan
    @Transactional
    public MessageResponse deleteMessage(String messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Message not found"));

        if (message.isDeleted()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Message already deleted");
        }

        if (!permissionService.canAccessChannel(message.getChannelId(), userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have access to this channel");
        }

        boolean isOwner = message.getSenderId().equals(userId);
        boolean canManageMessages = permissionService.canManageMessages(message.getChannelId(), userId);

        if (!isOwner && !canManageMessages) {
                throw new AppException(HttpStatus.FORBIDDEN, "You cannot delete this message");
        }

        message.setDeleted(true);
        message.setContent("");
        Message saved = messageRepository.save(message);

        MessageResponse response = toResponse(saved);

        messagingTemplate.convertAndSend(
                "/topic/channel/" + saved.getChannelId(),
                response
        );

        return response;
    }

    @Transactional
    public void markChannelAsRead(Long channelId, UUID userId, String messageId) {
    if (!permissionService.canAccessChannel(channelId, userId)) {
        throw new AppException(HttpStatus.FORBIDDEN, "You do not have access to this channel");
    }

    String targetMessageId = messageId;

    if (targetMessageId == null || targetMessageId.isBlank()) {
        targetMessageId = messageRepository
                .findTopByChannelIdAndDeletedFalseOrderByIdDesc(channelId)
                .map(Message::getId)
                .orElse(null);
    } else {
        Message message = messageRepository.findById(targetMessageId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!message.getChannelId().equals(channelId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Message does not belong to this channel");
        }
    }

    ChannelReadState state = channelReadStateRepository
            .findByChannelIdAndUserId(channelId, userId)
            .orElse(ChannelReadState.builder()
                    .channelId(channelId)
                    .userId(userId)
                    .build());

    state.setLastReadMessageId(targetMessageId);
    state.setLastReadAt(LocalDateTime.now());

    channelReadStateRepository.save(state);
}

    public ChannelUnreadStateResponse getUnreadState(Long channelId, UUID userId) {
        if (!permissionService.canAccessChannel(channelId, userId)) {
        throw new AppException(HttpStatus.FORBIDDEN, "You do not have access to this channel");
        }

    Optional<ChannelReadState> readStateOpt =
            channelReadStateRepository.findByChannelIdAndUserId(channelId, userId);

    Optional<Message> latestMessageOpt =
            messageRepository.findTopByChannelIdAndDeletedFalseOrderByIdDesc(channelId);

    if (latestMessageOpt.isEmpty()) {
        return ChannelUnreadStateResponse.builder()
                .channelId(channelId)
                .unread(false)
                .unreadCount(0)
                .mentionCount(0)
                .lastReadMessageId(readStateOpt.map(ChannelReadState::getLastReadMessageId).orElse(null))
                .latestMessageId(null)
                .build();
    }

    Message latestMessage = latestMessageOpt.get();

    long unreadCount;

    if (readStateOpt.isEmpty() || readStateOpt.get().getLastReadMessageId() == null) {
        unreadCount = messageRepository.countByChannelIdAndSenderIdNotAndDeletedFalse(
                channelId,
                userId
        );
    } else {
        unreadCount = messageRepository.countByChannelIdAndIdGreaterThanAndSenderIdNotAndDeletedFalse(
                channelId,
                readStateOpt.get().getLastReadMessageId(),
                userId
        );
    }

    return ChannelUnreadStateResponse.builder()
            .channelId(channelId)
            .unread(unreadCount > 0)
            .unreadCount(unreadCount)
            .mentionCount(0)
            .lastReadMessageId(readStateOpt.map(ChannelReadState::getLastReadMessageId).orElse(null))
            .latestMessageId(latestMessage.getId())
            .build();
}


}
