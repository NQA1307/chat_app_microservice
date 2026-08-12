package com.discordclone.messageservice.service;

import com.discordclone.common.event.DirectMessageSentEvent;
import com.discordclone.common.exception.AppException;
import com.discordclone.messageservice.dto.request.SendDirectMessageRequest;
import com.discordclone.messageservice.dto.request.UpdateMessageRequest;
import com.discordclone.messageservice.dto.response.ConversationResponse;
import com.discordclone.messageservice.dto.response.DirectMessageResponse;
import com.discordclone.messageservice.dto.response.DmConversationUpdatedEvent;
import com.discordclone.messageservice.dto.response.ReactionSummary;
import com.discordclone.messageservice.dto.response.ReplyPreview;
import com.discordclone.messageservice.entity.MessageMention;
import com.discordclone.messageservice.entity.ReactionSourceType;
import com.discordclone.messageservice.entity.Conversation;
import com.discordclone.messageservice.entity.DirectMessage;
import com.discordclone.messageservice.entity.MessageType;
import com.discordclone.messageservice.entity.MentionType;
import com.discordclone.messageservice.repository.ConversationRepository;
import com.discordclone.messageservice.repository.DirectMessageRepository;
import com.discordclone.messageservice.repository.MessageMentionRepository;
import com.discordclone.messageservice.security.MessageContentSanitizer;

import com.discordclone.messageservice.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DirectMessageService {

    private final ConversationRepository conversationRepository;
    private final DirectMessageRepository directMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageEventPublisher  messageEventPublisher;
    private final MessageReactionService messageReactionService;
    private final MessageContentSanitizer messageContentSanitizer;
    private final UserServiceClient userServiceClient;
    private final MentionParser mentionParser;
    private final MessageMentionRepository mentionRepository;
    private final TypingIndicatorService typingIndicatorService;

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

        UUID receiverId = conversation.getParticipantId1().equals(currentUserId)
                ? conversation.getParticipantId2()
                : conversation.getParticipantId1();

        UserServiceClient.BlockStatusResponse blockStatus = userServiceClient.checkBlockStatus(currentUserId, receiverId);
        if (blockStatus.blocked()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Cannot send direct message because this relationship is blocked.");
        }

        MessageType type = parseMessageType(req.getMessageType());
        validatePayload(type, req.getContent(), req.getMetadata(), req.getFileUrl(),
                req.getFileName(), req.getFileSize(), req.getContentType());

        if (req.getReplyToMessageId() != null && !req.getReplyToMessageId().isBlank()) {
        DirectMessage replyTo = directMessageRepository.findById(req.getReplyToMessageId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Reply target message not found"));

        if (!replyTo.getConversationId().equals(conversation.getId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Reply target must be in the same conversation");
        }
    }

        String sanitizedContent = type == MessageType.TEXT
        ? messageContentSanitizer.sanitizePlainText(req.getContent())
        : "";
        MentionParseResult mentions = mentionParser.parse(sanitizedContent);
        List<UUID> participantIds = List.of(conversation.getParticipantId1(), conversation.getParticipantId2());
        List<UUID> mentionedUserIds = mentions.userIds().stream()
                .filter(participantIds::contains)
                .filter(userId -> !userId.equals(currentUserId))
                .distinct()
                .toList();

        DirectMessage message = DirectMessage.builder()
                .conversationId(conversation.getId())
                .senderId(currentUserId)
                .senderUsername(username)
                .fileUrl(req.getFileUrl())
                .fileName(req.getFileName())
                .fileSize(req.getFileSize())
                .contentType(req.getContentType())
                .replyToMessageId(req.getReplyToMessageId())
                .messageType(type)
                .content(sanitizedContent)
                .metadata(req.getMetadata())
                .build();

        message = directMessageRepository.save(message);
        saveMentions(ReactionSourceType.DIRECT_MESSAGE, message.getId(), mentionedUserIds);

        DirectMessageResponse response = toDirectMessageResponse(message);

        messagingTemplate.convertAndSend(
                "/topic/dm." + conversation.getId(),
                response
        );

        LocalDateTime eventCreatedAt = response.getCreatedAt() != null
                ? response.getCreatedAt()
                : LocalDateTime.now();

        messageEventPublisher.publishDirectMessageSent(
                new DirectMessageSentEvent(
                        UUID.randomUUID().toString(),
                        response.getId(),
                        response.getConversationId(),
                        response.getSenderId(),
                        receiverId,
                        buildPreview(response.getContent()),
                        response.getContent(),
                        mentionedUserIds,
                        eventCreatedAt.atZone(ZoneId.systemDefault()).toInstant()
                )
        );

        DmConversationUpdatedEvent sidebarEvent = new DmConversationUpdatedEvent(
                response.getConversationId(),
                response.getSenderId(),
                receiverId,
                response.getSenderUsername(),
                buildPreview(response.getContent()),
                response.getCreatedAt()
        );

        messagingTemplate.convertAndSend(
                "/topic/user." + receiverId + ".dm-conversations",
                sidebarEvent
        );

        messagingTemplate.convertAndSend(
                "/topic/user." + currentUserId + ".dm-conversations",
                sidebarEvent
        );

        clearTypingAfterSend(conversation.getId(), currentUserId, username);

        return response;
    }

    private void clearTypingAfterSend(Long conversationId, UUID senderId, String senderUsername) {
        try {
            typingIndicatorService.clearTyping("DM", conversationId.toString(), senderId, senderUsername);
        } catch (RuntimeException ignored) {
            // Typing state is ephemeral; DM delivery must not depend on clearing it.
        }
    }

    private void saveMentions(
            ReactionSourceType sourceType,
            String sourceId,
            List<UUID> mentionedUserIds
    ) {
        if (mentionedUserIds == null || mentionedUserIds.isEmpty()) {
            return;
        }

        List<MessageMention> mentions = mentionedUserIds.stream()
                .map(userId -> MessageMention.builder()
                        .sourceType(sourceType)
                        .sourceId(sourceId)
                        .mentionedUserId(userId)
                        .mentionType(MentionType.USER)
                        .build())
                .toList();

        mentionRepository.saveAll(mentions);
    }

    private String buildPreview(String content) {
        if (content == null || content.isBlank()) {
            return "Sent an attachment";
        }
        return content.length() <= 120 ? content :content.substring(0,120);
    }

    public List<DirectMessageResponse> getDirectMessages(Long conversationId, UUID currentUserId, int page, int size) {
        return getDirectMessages(conversationId, currentUserId, page, size, null);
    }

    public List<DirectMessageResponse> getDirectMessages(
            Long conversationId,
            UUID currentUserId,
            int page,
            int size,
            String beforeId
    ) {
        getConversationForParticipant(conversationId, currentUserId);

        int safeSize = Math.min(Math.max(size, 1), 100);
        List<DirectMessage> messages;
        if (beforeId != null && !beforeId.isBlank()) {
            messages = directMessageRepository.findConversationMessagesBefore(
                    conversationId,
                    beforeId,
                    PageRequest.of(0, safeSize)
            );
        } else {
            messages = directMessageRepository
                    .findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(
                            conversationId,
                            PageRequest.of(Math.max(page, 0), safeSize)
                    )
                    .getContent();
        }

        return toDirectMessageResponses(messages);
    }

    public List<DirectMessageResponse> searchDirectMessages(
            Long conversationId,
            UUID currentUserId,
            String query,
            int page,
            int size
    ) {
        getConversationForParticipant(conversationId, currentUserId);

        String normalizedQuery = normalizeSearchQuery(query);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        List<DirectMessage> messages = directMessageRepository.searchDirectMessages(
                    conversationId,
                    normalizedQuery,
                    PageRequest.of(safePage, safeSize)
                )
                .getContent();
        return toDirectMessageResponses(messages);
    }

    private String normalizeSearchQuery(String query) {
        if (query == null || query.trim().isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Search query is required");
        }
        return query.trim().toLowerCase();
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
        return toDirectMessageResponse(
                m,
                messageReactionService.getSummary(ReactionSourceType.DIRECT_MESSAGE, m.getId()),
                buildDirectReplyPreview(m.getReplyToMessageId(), m.getConversationId())
        );
    }

    private List<DirectMessageResponse> toDirectMessageResponses(List<DirectMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        List<String> messageIds = messages.stream()
                .map(DirectMessage::getId)
                .toList();
        Map<String, List<ReactionSummary>> loadedReactionsByMessage =
                messageReactionService.getSummaryMap(ReactionSourceType.DIRECT_MESSAGE, messageIds);
        Map<String, List<ReactionSummary>> reactionsByMessage = loadedReactionsByMessage == null
                ? Map.of()
                : loadedReactionsByMessage;

        Set<String> replyIds = messages.stream()
                .map(DirectMessage::getReplyToMessageId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        Map<String, DirectMessage> repliesById = replyIds.isEmpty()
                ? Collections.emptyMap()
                : directMessageRepository.findAllById(replyIds)
                        .stream()
                        .collect(Collectors.toMap(DirectMessage::getId, Function.identity()));

        return messages.stream()
                .map(message -> toDirectMessageResponse(
                        message,
                        reactionsByMessage.getOrDefault(message.getId(), List.of()),
                        buildDirectReplyPreview(message.getReplyToMessageId(), message.getConversationId(), repliesById)
                ))
                .toList();
    }

    private DirectMessageResponse toDirectMessageResponse(
            DirectMessage m,
            List<ReactionSummary> reactions,
            ReplyPreview replyPreview
    ) {
        return DirectMessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .senderUsername(m.getSenderUsername())
                .content(m.getContent())
                .fileUrl(m.getFileUrl())
                .fileName(m.getFileName())
                .fileSize(m.getFileSize())
                .contentType(m.getContentType())
                .deleted(m.isDeleted())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .replyToMessageId(m.getReplyToMessageId())
                .replyTo(replyPreview)
                .reactions(reactions)
                .messageType(m.getMessageType().name())
                .metadata(m.getMetadata())
                .build();
    }

    private ReplyPreview buildDirectReplyPreview(String replyToMessageId, Long conversationId) {
        if (replyToMessageId == null || replyToMessageId.isBlank()) {
            return null;
        }

        return directMessageRepository.findById(replyToMessageId)
                .filter(message -> message.getConversationId().equals(conversationId))
                .map(message -> new ReplyPreview(
                        message.getId(),
                        message.getSenderId(),
                        message.getSenderUsername(),
                        message.isDeleted() ? "" : message.getContent(),
                        message.isDeleted()
                ))
                .orElse(null);
    }

    private ReplyPreview buildDirectReplyPreview(
            String replyToMessageId,
            Long conversationId,
            Map<String, DirectMessage> repliesById
    ) {
        if (replyToMessageId == null || replyToMessageId.isBlank()) {
            return null;
        }

        DirectMessage message = repliesById.get(replyToMessageId);
        if (message == null || !message.getConversationId().equals(conversationId)) {
            return null;
        }

        return new ReplyPreview(
                message.getId(),
                message.getSenderId(),
                message.getSenderUsername(),
                message.isDeleted() ? "" : message.getContent(),
                message.isDeleted()
        );
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

    String content = messageContentSanitizer.sanitizePlainText(req.getContent());
    if (content.isBlank() && message.getFileUrl() == null) {
        throw new AppException(HttpStatus.BAD_REQUEST, "Message content cannot be empty");
    }

    message.setContent(content);
    DirectMessage saved = directMessageRepository.save(message);
    DirectMessageResponse response = toDirectMessageResponse(saved);

    messagingTemplate.convertAndSend(
            "/topic/dm." + saved.getConversationId(),
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
            "/topic/dm." + saved.getConversationId(),
            response
    );

    return response;
}



    private MessageType parseMessageType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return MessageType.TEXT;
        }

        try {
            return MessageType.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid message type");
        }
    }

    private void validatePayload(
            MessageType type,
            String content,
            String metadata,
            String fileUrl,
            String fileName,
            Long fileSize,
            String contentType
    ) {
        if (type == MessageType.TEXT && isBlank(content)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Message content is required");
        }

        if (type == MessageType.FILE) {
            if (isBlank(fileUrl) || isBlank(fileName) || fileSize == null || fileSize <= 0 || isBlank(contentType)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Invalid attachment metadata");
            }
        }

        if ((type == MessageType.STICKER || type == MessageType.GIF) && isBlank(metadata)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Message metadata is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
