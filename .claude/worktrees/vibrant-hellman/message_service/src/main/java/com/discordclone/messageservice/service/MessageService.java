package com.discordclone.messageservice.service;

import com.discordclone.common.event.MessageSentEvent;
import com.discordclone.common.exception.AppException;
import com.discordclone.messageservice.client.ServerServiceClient;
import com.discordclone.messageservice.dto.request.SendMessageRequest;
import com.discordclone.messageservice.dto.request.UpdateMessageRequest;
import com.discordclone.messageservice.dto.response.ChannelUnreadStateResponse;
import com.discordclone.messageservice.dto.response.MessageResponse;
import com.discordclone.messageservice.dto.response.ReactionSummary;
import com.discordclone.messageservice.dto.response.ReplyPreview;
import com.discordclone.messageservice.entity.Message;
import com.discordclone.messageservice.entity.MessageMention;
import com.discordclone.messageservice.entity.MessageType;
import com.discordclone.messageservice.entity.MentionType;
import com.discordclone.messageservice.entity.ReactionSourceType;
import com.discordclone.messageservice.repository.MessageRepository;
import com.discordclone.messageservice.security.MessageContentSanitizer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.discordclone.messageservice.entity.ChannelReadState;
import com.discordclone.messageservice.repository.ChannelReadStateRepository;
import com.discordclone.messageservice.repository.MessageMentionRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageReactionService messageReactionService;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PermissionService permissionService;
    private final MessageEventPublisher messageEventPublisher;
    private final ChannelReadStateRepository channelReadStateRepository;
    private final MessageContentSanitizer messageContentSanitizer;
    private final MentionParser mentionParser;
    private final MessageMentionRepository mentionRepository;
    private final ServerServiceClient serverServiceClient;
    private final PresenceService presenceService;
    private final StringRedisTemplate redisTemplate;
    private final TypingIndicatorService typingIndicatorService;

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest req) {

        


        if (req.getSenderId() == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Missing sender id");
        }

        if (req.getSenderUsername() == null || req.getSenderUsername().isBlank()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Missing sender username");
        }

        // Validate send permission before persisting or broadcasting a channel message.
        if (!permissionService.canSendMessage(req.getChannelId(), req.getSenderId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have permission to send messages in this channel");
        }

        MessageType type = parseMessageType(req.getMessageType());
        String sanitizedContent = type == MessageType.TEXT
        ? messageContentSanitizer.sanitizePlainText(req.getContent())
        : "";
        MentionParseResult mentions = mentionParser.parse(sanitizedContent);
        
        if ((mentions.mentionEveryone() || mentions.mentionHere())
        && !permissionService.canMentionEveryone(req.getChannelId(), req.getSenderId())) {
        throw new AppException(HttpStatus.FORBIDDEN, "You do not have permission to mention everyone");
        }

        if (mentions.mentionEveryone() || mentions.mentionHere()) {
            enforceBroadcastMentionCooldown(req.getChannelId(), req.getSenderId());
        }
        
        validatePayload(type, req.getContent(), req.getMetadata(), req.getFileUrl(),
                req.getFileName(), req.getFileSize(), req.getContentType());

        if (req.getReplyToMessageId() != null && !req.getReplyToMessageId().isBlank()) {
            Message replyTo = messageRepository.findById(req.getReplyToMessageId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Reply target message not found"));

            if (!replyTo.getChannelId().equals(req.getChannelId())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Reply target must be in the same channel");
            }
        }

        

        Message message = Message.builder()
                .channelId(req.getChannelId())
                .senderId(req.getSenderId())
                .senderUsername(req.getSenderUsername())
                .fileUrl(req.getFileUrl())
                .fileName(req.getFileName())
                .fileSize(req.getFileSize())
                .contentType(req.getContentType())
                .replyToMessageId(req.getReplyToMessageId())
                .messageType(type)
                .content(sanitizedContent)
                .metadata(req.getMetadata())
                .build();

        message = messageRepository.save(message);
        List<UUID> recipientIds = resolveRecipientIds(req.getChannelId(), req.getSenderId());
        List<UUID> mentionedUserIds = resolveMentionedUserIds(
                mentions,
                recipientIds,
                req.getSenderId(),
                req.getChannelId()
        );
        saveMentions(ReactionSourceType.CHANNEL_MESSAGE, message.getId(), mentionedUserIds);
        saveBroadcastMentionMarkers(ReactionSourceType.CHANNEL_MESSAGE, message.getId(), mentions);
        MessageResponse response = toResponse(message);

        messagingTemplate.convertAndSend("/topic/channel." + req.getChannelId(), response);

        messageEventPublisher.publishMessageSent(MessageSentEvent.builder()
                .messageId(message.getId())
                .channelId(message.getChannelId())
                .senderId(message.getSenderId())
                .senderUsername(message.getSenderUsername())
                .content(buildPreview(sanitizedContent))
                .contentPreview(buildPreview(sanitizedContent))
                .createdAt(message.getCreatedAt())
                .recipientIds(recipientIds)
                .mentionedUserIds(mentionedUserIds)
                .mentionEveryone(mentions.mentionEveryone())
                .mentionHere(mentions.mentionHere())
                .build());

        clearTypingAfterSend(req.getChannelId(), req.getSenderId(), req.getSenderUsername());

        return response;
    }

    private void clearTypingAfterSend(Long channelId, UUID senderId, String senderUsername) {
        try {
            typingIndicatorService.clearTyping("CHANNEL", channelId.toString(), senderId, senderUsername);
        } catch (RuntimeException ignored) {
            // Typing state is ephemeral; message delivery must not depend on clearing it.
        }
    }

    private List<UUID> resolveRecipientIds(Long channelId, UUID senderId) {
        List<UUID> memberIds = serverServiceClient.getChannelMemberIds(channelId);
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of(senderId);
        }
        return memberIds.stream().distinct().toList();
    }

    private List<UUID> filterMentionedUserIds(
            List<UUID> parsedMentionedUserIds,
            List<UUID> recipientIds,
            UUID senderId
    ) {
        if (parsedMentionedUserIds == null || parsedMentionedUserIds.isEmpty()) {
            return List.of();
        }

        Set<UUID> recipientSet = new HashSet<>(recipientIds);
        return parsedMentionedUserIds.stream()
                .filter(recipientSet::contains)
                .filter(userId -> !userId.equals(senderId))
                .distinct()
                .toList();
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

    private void saveBroadcastMentionMarkers(
            ReactionSourceType sourceType,
            String sourceId,
            MentionParseResult mentions
    ) {
        if (mentions.mentionEveryone()) {
            saveMentionMarker(sourceType, sourceId, MentionType.EVERYONE);
        }
        if (mentions.mentionHere()) {
            saveMentionMarker(sourceType, sourceId, MentionType.HERE);
        }
    }

    private void saveMentionMarker(
            ReactionSourceType sourceType,
            String sourceId,
            MentionType mentionType
    ) {
        mentionRepository.save(MessageMention.builder()
                .sourceType(sourceType)
                .sourceId(sourceId)
                .mentionType(mentionType)
                .build());
    }

    private void enforceBroadcastMentionCooldown(Long channelId, UUID senderId) {
        String key = "mention:broadcast:" + channelId + ":" + senderId;
        Boolean firstUse = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(60));
        if (Boolean.FALSE.equals(firstUse)) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before mentioning everyone or here again");
        }
    }

    private String buildPreview(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String normalized = content.trim().replaceAll("\\s+", " ");
        int maxLength = 120;
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    public List<MessageResponse> getMessages(Long channelId, UUID userId, int page, int size) {
        return getMessages(channelId, userId, page, size, null);
    }

    public List<MessageResponse> getMessages(Long channelId, UUID userId, int page, int size, String beforeId) {
        // History must be protected too, not only send-message.
        if (!permissionService.canAccessChannel(channelId, userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have access to this channel");
        }

        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Message> messages;

        if (beforeId != null && !beforeId.isBlank()) {
            messages = messageRepository.findChannelMessagesBefore(
                    channelId,
                    beforeId,
                    PageRequest.of(0, safeSize)
            );
        } else {
            messages = messageRepository
                    .findByChannelIdAndDeletedFalseOrderByCreatedAtDesc(
                            channelId,
                            PageRequest.of(Math.max(page, 0), safeSize)
                    )
                    .getContent();
        }

        return toResponses(messages);
    }

    public List<MessageResponse> searchMessages(Long channelId, UUID userId, String query, int page, int size) {
        if (!permissionService.canAccessChannel(channelId, userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have access to this channel");
        }

        String normalizedQuery = normalizeSearchQuery(query);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        List<Message> messages = messageRepository.searchChannelMessages(
                    channelId,
                    normalizedQuery,
                    PageRequest.of(safePage, safeSize)
                )
                .getContent();

        return toResponses(messages);
    }

    private String normalizeSearchQuery(String query) {
        if (query == null || query.trim().isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Search query is required");
        }
        return query.trim().toLowerCase();
    }

    private List<MessageResponse> toResponses(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        List<String> messageIds = messages.stream()
                .map(Message::getId)
                .toList();
        Map<String, List<ReactionSummary>> loadedReactionsByMessage =
                messageReactionService.getSummaryMap(ReactionSourceType.CHANNEL_MESSAGE, messageIds);
        Map<String, List<ReactionSummary>> reactionsByMessage = loadedReactionsByMessage == null
                ? Map.of()
                : loadedReactionsByMessage;

        Set<String> replyIds = messages.stream()
                .map(Message::getReplyToMessageId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        Map<String, Message> repliesById = replyIds.isEmpty()
                ? Collections.emptyMap()
                : messageRepository.findAllById(replyIds)
                        .stream()
                        .collect(Collectors.toMap(Message::getId, Function.identity()));

        return messages.stream()
                .map(message -> toResponse(
                        message,
                        reactionsByMessage.getOrDefault(message.getId(), List.of()),
                        buildChannelReplyPreview(message.getReplyToMessageId(), message.getChannelId(), repliesById)
                ))
                .toList();
    }

    private MessageResponse toResponse(Message m) {
        return toResponse(
                m,
                messageReactionService.getSummary(ReactionSourceType.CHANNEL_MESSAGE, m.getId()),
                buildChannelReplyPreview(m.getReplyToMessageId(), m.getChannelId())
        );
    }

    private MessageResponse toResponse(
            Message m,
            List<ReactionSummary> reactions,
            ReplyPreview replyPreview
    ) {
        return MessageResponse.builder()
                .id(m.getId())
                .channelId(m.getChannelId())
                .senderId(m.getSenderId())
                .senderUsername(m.getSenderUsername())
                .content(m.getContent())
                .fileUrl(m.getFileUrl())
                .fileName(m.getFileName())
                .fileSize(m.getFileSize())
                .contentType(m.getContentType())
                .deleted(m.isDeleted())
                .createdAt(m.getCreatedAt())
                .replyToMessageId(m.getReplyToMessageId())
                .replyTo(replyPreview)
                .reactions(reactions)
                .messageType(m.getMessageType().name())
                .metadata(m.getMetadata())
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

        String content = messageContentSanitizer.sanitizePlainText(req.getContent());
        if (content.isBlank() && message.getFileUrl() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Message content cannot be empty");
        }

        message.setContent(content);
        Message saved = messageRepository.save(message);

        MessageResponse response = toResponse(saved);

        messagingTemplate.convertAndSend(
                "/topic/channel." + saved.getChannelId(),
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
                "/topic/channel." + saved.getChannelId(),
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

    private ReplyPreview buildChannelReplyPreview(String replyToMessageId, Long channelId) {
    if (replyToMessageId == null || replyToMessageId.isBlank()) return null;

    return messageRepository.findById(replyToMessageId)
            .filter(message -> message.getChannelId().equals(channelId))
            .map(message -> new ReplyPreview(
                    message.getId(),
                    message.getSenderId(),
                    message.getSenderUsername(),
                    message.isDeleted() ? "" : message.getContent(),
                    message.isDeleted()
            ))
            .orElse(null);
    }

    private ReplyPreview buildChannelReplyPreview(
            String replyToMessageId,
            Long channelId,
            Map<String, Message> repliesById
    ) {
    if (replyToMessageId == null || replyToMessageId.isBlank()) return null;

    Message message = repliesById.get(replyToMessageId);
    if (message == null || !message.getChannelId().equals(channelId)) return null;

    return new ReplyPreview(
            message.getId(),
            message.getSenderId(),
            message.getSenderUsername(),
            message.isDeleted() ? "" : message.getContent(),
            message.isDeleted()
    );
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

    //Helper loc member khi mention everyone
    private List<UUID> resolveMentionedUserIds(
            MentionParseResult mentions,
            List<UUID> recipientIds,
            UUID senderId,
            Long channelId
    ) {
        Set<UUID> result = new LinkedHashSet<>();

        result.addAll(filterMentionedUserIds(mentions.userIds(), recipientIds, senderId));

        if (mentions.mentionEveryone()) {
            recipientIds.stream()
                    .filter(id -> !id.equals(senderId))
                    .forEach(result::add);
        }

        if (mentions.mentionHere()) {
            List<UUID> onlineRecipientIds = presenceService.filterOnlineUsers(recipientIds);
            onlineRecipientIds.stream()
                    .filter(id -> !id.equals(senderId))
                    .forEach(result::add);
        }

        return result.stream().toList();
    }
}
