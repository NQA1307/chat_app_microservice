package com.discordclone.messageservice.service;

import com.discordclone.common.exception.AppException;
import com.discordclone.messageservice.dto.request.SetReactionRequest;
import com.discordclone.messageservice.dto.response.MessageReactionEvent;
import com.discordclone.messageservice.dto.response.ReactionSummary;
import com.discordclone.messageservice.dto.response.ReactionUpdateResponse;
import com.discordclone.messageservice.entity.*;
import com.discordclone.messageservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageReactionService {

    private final MessageRepository messageRepository;
    private final DirectMessageRepository directMessageRepository;
    private final ConversationRepository conversationRepository;
    private final MessageReactionRepository reactionRepository;
    private final PermissionService permissionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageReactionEventPublisher eventPublisher;

    @Transactional
    public ReactionUpdateResponse setChannelReaction(
        String messageId,
        UUID userId,
        SetReactionRequest request
    ) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Message not found"));

        if (message.isDeleted()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot react to deleted message");
        }

        if (!permissionService.canAccessChannel(message.getChannelId(), userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have access to this channel");
        }

        ReactionUpdateResponse response = setReaction(
            ReactionSourceType.CHANNEL_MESSAGE,
            messageId,
            userId,
            request.emoji()
        );

        MessageReactionEvent event = new MessageReactionEvent(
            UUID.randomUUID().toString(),
            ReactionSourceType.CHANNEL_MESSAGE.name(),
            messageId,
            message.getChannelId(),
            null,
            userId,
            response.myReaction(),
            response.action(),
            response.reactions()
        );

        messagingTemplate.convertAndSend(
            "/topic/channel." + message.getChannelId() + ".reactions",
            event
        );

        eventPublisher.publishReactionUpdated(event);

        return response;
    }

    @Transactional
    public ReactionUpdateResponse removeChannelReaction(String messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!permissionService.canAccessChannel(message.getChannelId(), userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have access to this channel");
        }

        ReactionUpdateResponse response = removeReaction(
            ReactionSourceType.CHANNEL_MESSAGE,
            messageId,
            userId
        );

        MessageReactionEvent event = new MessageReactionEvent(
            UUID.randomUUID().toString(),
            ReactionSourceType.CHANNEL_MESSAGE.name(),
            messageId,
            message.getChannelId(),
            null,
            userId,
            null,
            response.action(),
            response.reactions()
        );

        messagingTemplate.convertAndSend(
            "/topic/channel." + message.getChannelId() + ".reactions",
            event
        );

        eventPublisher.publishReactionUpdated(event);

        return response;
    }

    @Transactional
    public ReactionUpdateResponse setDirectReaction(
        String messageId,
        UUID userId,
        SetReactionRequest request
    ) {
        DirectMessage message = directMessageRepository.findById(messageId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Message not found"));

        if (message.isDeleted()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot react to deleted message");
        }

        Conversation conversation = conversationRepository.findById(message.getConversationId())
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!isParticipant(conversation, userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You are not a participant of this conversation");
        }

        ReactionUpdateResponse response = setReaction(
            ReactionSourceType.DIRECT_MESSAGE,
            messageId,
            userId,
            request.emoji()
        );

        MessageReactionEvent event = new MessageReactionEvent(
            UUID.randomUUID().toString(),
            ReactionSourceType.DIRECT_MESSAGE.name(),
            messageId,
            null,
            message.getConversationId(),
            userId,
            response.myReaction(),
            response.action(),
            response.reactions()
        );

        messagingTemplate.convertAndSend(
            "/topic/dm." + message.getConversationId() + ".reactions",
            event
        );

        eventPublisher.publishReactionUpdated(event);

        return response;
    }

    @Transactional
    public ReactionUpdateResponse removeDirectReaction(String messageId, UUID userId) {
        DirectMessage message = directMessageRepository.findById(messageId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Message not found"));

        Conversation conversation = conversationRepository.findById(message.getConversationId())
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!isParticipant(conversation, userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You are not a participant of this conversation");
        }

        ReactionUpdateResponse response = removeReaction(
            ReactionSourceType.DIRECT_MESSAGE,
            messageId,
            userId
        );

        MessageReactionEvent event = new MessageReactionEvent(
            UUID.randomUUID().toString(),
            ReactionSourceType.DIRECT_MESSAGE.name(),
            messageId,
            null,
            message.getConversationId(),
            userId,
            null,
            response.action(),
            response.reactions()
        );

        messagingTemplate.convertAndSend(
            "/topic/dm." + message.getConversationId() + ".reactions",
            event
        );

        eventPublisher.publishReactionUpdated(event);

        return response;
    }

    private ReactionUpdateResponse setReaction(
        ReactionSourceType sourceType,
        String sourceId,
        UUID userId,
        String emoji
    ) {
        String cleanEmoji = validateEmoji(emoji);

        Optional<MessageReaction> existingOpt =
            reactionRepository.findBySourceTypeAndSourceIdAndUserId(sourceType, sourceId, userId);

        String action;

        if (existingOpt.isEmpty()) {
            reactionRepository.save(MessageReaction.builder()
                .sourceType(sourceType)
                .sourceId(sourceId)
                .userId(userId)
                .emoji(cleanEmoji)
                .build());
            action = "ADDED";
        } else {
            MessageReaction existing = existingOpt.get();
            if (existing.getEmoji().equals(cleanEmoji)) {
                reactionRepository.delete(existing);
                action = "REMOVED";
            } else {
                existing.setEmoji(cleanEmoji);
                reactionRepository.save(existing);
                action = "UPDATED";
            }
        }

        List<ReactionSummary> summary = getSummary(sourceType, sourceId);
        String myReaction = action.equals("REMOVED") ? null : cleanEmoji;

        return new ReactionUpdateResponse(
            sourceType.name(),
            sourceId,
            myReaction,
            action,
            summary
        );
    }

    private ReactionUpdateResponse removeReaction(
        ReactionSourceType sourceType,
        String sourceId,
        UUID userId
    ) {
        reactionRepository.deleteBySourceTypeAndSourceIdAndUserId(sourceType, sourceId, userId);

        return new ReactionUpdateResponse(
            sourceType.name(),
            sourceId,
            null,
            "REMOVED",
            getSummary(sourceType, sourceId)
        );
    }

    public List<ReactionSummary> getSummary(ReactionSourceType sourceType, String sourceId) {
        return reactionRepository.findBySourceTypeAndSourceId(sourceType, sourceId)
            .stream()
            .collect(Collectors.groupingBy(MessageReaction::getEmoji, Collectors.counting()))
            .entrySet()
            .stream()
            .map(entry -> new ReactionSummary(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(ReactionSummary::emoji))
            .toList();
    }

    public Map<String, List<ReactionSummary>> getSummaryMap(ReactionSourceType sourceType, List<String> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Map.of();
        }

        return reactionRepository.summarizeBySourceIds(sourceType, sourceIds.stream().distinct().toList())
                .stream()
                .collect(Collectors.groupingBy(
                        MessageReactionRepository.ReactionSummaryRow::getSourceId,
                        Collectors.mapping(
                                row -> new ReactionSummary(row.getEmoji(), row.getCount()),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> list.stream()
                                                .sorted(Comparator.comparing(ReactionSummary::emoji))
                                                .toList()
                                )
                        )
                ));
    }

    private boolean isParticipant(Conversation conversation, UUID userId) {
        return conversation.getParticipantId1().equals(userId)
            || conversation.getParticipantId2().equals(userId);
    }

    private String validateEmoji(String emoji) {
        if (emoji == null || emoji.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Emoji is required");
        }

        String clean = emoji.trim();

        if (clean.length() > 100) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Emoji is too long");
        }

        return clean;
    }
}
