package com.discordclone.notificationservice.service;

import com.discordclone.common.event.DirectMessageSentEvent;
import com.discordclone.common.event.EventTypes;
import com.discordclone.common.event.FriendRequestEvent;
import com.discordclone.common.event.MessageSentEvent;
import com.discordclone.common.event.NotificationCreatedEvent;
import com.discordclone.common.event.NotificationReadEvent;
import com.discordclone.common.event.ServerInviteEvent;
import com.discordclone.common.event.ServerMemberEvent;
import com.discordclone.common.exception.AppException;
import com.discordclone.notificationservice.dto.NotificationResponse;
import com.discordclone.notificationservice.entity.Notification;
import com.discordclone.notificationservice.repository.NotificationRepository;
import de.huxhorn.sulky.ulid.ULID;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ProcessedEventService processedEventService;
    private final NotificationRepository notificationRepository;
    private final ULID ulid = new ULID();
    private final DomainEventPublisher domainEventPublisher;
    private final PushNotificationService pushNotificationService;
    private final NotificationSettingsService settingsService;

    @Value("${app.rabbitmq.notification-read-routing-key}")
    private String notificationReadRoutingKey;

    @Value("${app.rabbitmq.notification-created-routing-key}")
    private String notificationCreatedRoutingKey;

    public List<NotificationResponse> getNotifications(UUID userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public long countUnread(UUID userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }


    //
    @Transactional
    public NotificationResponse markAsRead(String notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getRecipientId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You cannot access this notification");
        }

        notification.setRead(true);

        Notification saved = notificationRepository.save(notification);

        publishRead(saved.getRecipientId(), saved.getId(), saved.getSourceType(), saved.getSourceId());

        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    //Tao event message.sent
    @Transactional
    public void createFromMessageSent(MessageSentEvent event, UUID eventId) {
        if (!processedEventService.tryMarkProcessing(eventId, EventTypes.MESSAGE_SENT)) {
            return;
        }

        if (event.getRecipientIds() == null || event.getRecipientIds().isEmpty()) {
            return;
        }

        Set<UUID> mentionedUserIds = event.getMentionedUserIds() == null
                ? Set.of()
                : new HashSet<>(event.getMentionedUserIds());

        if (mentionedUserIds.isEmpty()) {
            return;
        }

        List<Notification> notifications = event.getRecipientIds().stream()
                .filter(recipientId -> !recipientId.equals(event.getSenderId()))
                .filter(mentionedUserIds::contains)
                .map(recipientId -> Notification.builder()
                        .id(ulid.nextULID())
                        .recipientId(recipientId)
                        .type("MENTION")
                        .title(mentionTitle(event))
                        .body(messagePreview(event))
                        .read(false)
                        .sourceType("MESSAGE")
                        .sourceId(event.getMessageId())
                        .build())
                .toList();

        List<Notification> saved = notificationRepository.saveAll(notifications);
        Set<UUID> recipientIds = saved.stream()
                .map(Notification::getRecipientId)
                .collect(Collectors.toSet());
        Map<UUID, com.discordclone.notificationservice.entity.NotificationSettings> settingsByRecipient =
                settingsService.getSettingsForUsers(recipientIds);

        saved.forEach(notification -> publishCreated(notification, settingsByRecipient));
        
    }

    //Map sang response
    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .read(notification.isRead())
                .sourceType(notification.getSourceType())
                .sourceId(notification.getSourceId())
                .createdAt(notification.getCreatedAt())
                .build();
    }


    //Tao noti tu event gui DM
    @Transactional
    public void createFromDirectMessageSent(DirectMessageSentEvent event, UUID eventId) {
        if (!processedEventService.tryMarkProcessing(eventId, EventTypes.DM_MESSAGE_SENT)) {
            return;
        }

        if (event.receiverId().equals(event.senderId())) {
            return;
        }

        boolean mentioned = event.mentionedUserIds() != null
                && event.mentionedUserIds().contains(event.receiverId());

        Notification notification = Notification.builder()
                .id(ulid.nextULID())
                .recipientId(event.receiverId())
                .type(mentioned ? "DM_MENTION" : "DM_MESSAGE")
                .title(mentioned ? "You were mentioned in a DM" : "New direct message")
                .body(event.preview())
                .read(false)
                .sourceType("DIRECT_MESSAGE")
                .sourceId(String.valueOf(event.conversationId()))
                .build();

        notificationRepository.save(notification);

        publishCreated(notification);
    }

    //Tao noti tu event gui loi moi tham gia server
    @Transactional
    public void createFromServerInvite(ServerInviteEvent event, UUID eventId) {
        if (!processedEventService.tryMarkProcessing(eventId, EventTypes.SERVER_INVITE_SENT)) {
            return;
        }

        Notification notification = Notification.builder()
                .id(ulid.nextULID())
                .recipientId(event.receiverId())
                .type("SERVER_INVITE")
                .title("Server invite")
                .body("You were invited to " + event.serverName())
                .read(false)
                .sourceType("SERVER_INVITE")
                .sourceId(event.inviteId())
                .build();

        notificationRepository.save(notification);
        publishCreated(notification);
    }

    @Transactional
    public void createFromFriendRequest(FriendRequestEvent event, UUID eventId) {
        String eventType = "ACCEPTED".equals(event.type())
            ? EventTypes.FRIEND_REQUEST_ACCEPTED
            : EventTypes.FRIEND_REQUEST_SENT;

        if (!processedEventService.tryMarkProcessing(eventId, eventType)) {
            return;
        }

        String type = "FRIEND_REQUEST";
        String title = "Friend request";
        String body = event.senderUsername() + " sent you a friend request";

        if ("ACCEPTED".equals(event.type())) {
            type = "FRIEND_ACCEPTED";
            title = "Friend request accepted";
            body = event.senderUsername() + " accepted your friend request";
        }

        Notification notification = Notification.builder()
                .id(ulid.nextULID())
                .recipientId(event.receiverId())
                .type(type)
                .title(title)
                .body(body)
                .read(false)
                .sourceType("FRIEND")
                .sourceId(String.valueOf(event.senderId()))
                .build();

        notificationRepository.save(notification);
        publishCreated(notification);
    }

    @Transactional
    public void createFromServerMemberAction(ServerMemberEvent event, UUID eventId) {
        String eventType = "KICKED".equals(event.action())
            ? EventTypes.SERVER_MEMBER_KICKED
            : EventTypes.SERVER_MEMBER_LEFT;

        if (!processedEventService.tryMarkProcessing(eventId, eventType)) {
            return;
        }

        String type = "SERVER_MEMBER_ACTION";
        String title = "Server update";
        String body = "";

        if ("KICKED".equals(event.action())) {
            type = "SERVER_KICKED";
            title = "Kicked from server";
            body = "You were kicked from " + event.serverName();
        } else if ("LEFT".equals(event.action())) {
            // Usually we don't notify the person who left, but we might notify the owner?
            // For now, let's just ignore LEFT unless it's for something else
            return;
        }

        Notification notification = Notification.builder()
                .id(ulid.nextULID())
                .recipientId(event.targetId())
                .type(type)
                .title(title)
                .body(body)
                .read(false)
                .sourceType("SERVER")
                .sourceId(String.valueOf(event.serverId()))
                .build();

        notificationRepository.save(notification);
        publishCreated(notification);
    }

    @Transactional
    public int markSourceAsRead(UUID userId, String sourceType, String sourceId) {
        int updated = notificationRepository.markSourceAsRead(userId, sourceType, sourceId);

        if (updated > 0) {
            publishRead(userId, null, sourceType, sourceId);
        }
        return updated;
    }

    //Tao event noti.created
    private void publishCreated(Notification notification) {
        publishCreated(notification, Map.of());
    }

    private void publishCreated(
            Notification notification,
            Map<UUID, com.discordclone.notificationservice.entity.NotificationSettings> settingsByRecipient
    ) {
        NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .read(notification.isRead())
                .sourceType(notification.getSourceType())
                .sourceId(notification.getSourceId())
                .createdAt(notification.getCreatedAt())
                .build();

        domainEventPublisher.publish(
                EventTypes.NOTIFICATION_CREATED,
                notificationCreatedRoutingKey,
                event
        );

        // Send push notification via Expo if enabled in settings
        com.discordclone.notificationservice.entity.NotificationSettings settings =
                settingsByRecipient.get(notification.getRecipientId());

        if (settings == null) {
            settings = settingsService.getSettings(notification.getRecipientId());
        }
        
        boolean shouldPush = shouldSendPush(notification, settings);

        if (shouldPush) {
            pushNotificationService.sendPush(
                    notification.getRecipientId(),
                    notification.getTitle(),
                    notification.getBody(),
                    java.util.Map.of(
                            "sourceType", notification.getSourceType() == null ? "" : notification.getSourceType(),
                            "sourceId", notification.getSourceId() == null ? "" : notification.getSourceId(),
                            "type", notification.getType()
                    )
            );
        }
    }

    private boolean shouldSendPush(
            Notification notification,
            com.discordclone.notificationservice.entity.NotificationSettings settings
    ) {
        return switch (notification.getType()) {
            case "DM", "DM_MESSAGE", "DM_MENTION" -> settings.isDmEnabled();
            case "SERVER_INVITE", "SERVER_MEMBER_ACTION", "SERVER_KICKED", "SERVER_MEMBER_KICKED" ->
                    settings.isServerEnabled();
            case "FRIEND_REQUEST", "FRIEND_ACCEPTED" -> settings.isFriendRequestEnabled();
            default -> true;
        };
    }

    private String messagePreview(MessageSentEvent event) {
        if (event.getContentPreview() != null && !event.getContentPreview().isBlank()) {
            return event.getContentPreview();
        }
        return event.getContent() == null ? "" : event.getContent();
    }

    private String mentionTitle(MessageSentEvent event) {
        if (event.isMentionEveryone()) {
            return event.getSenderUsername() + " mentioned everyone";
        }
        if (event.isMentionHere()) {
            return event.getSenderUsername() + " mentioned here";
        }
        return event.getSenderUsername() + " mentioned you";
    }

    //publish read event
    private void publishRead(
        UUID recipientId,
        String notificationId,
        String sourceType,
        String sourceId
    ) {
        long unreadCount = notificationRepository.countByRecipientIdAndReadFalse(recipientId);

        NotificationReadEvent event = NotificationReadEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .recipientId(recipientId)
                        .notificationId(notificationId)
                        .sourceType(sourceType)
                        .sourceId(sourceId)
                        .unreadCount(unreadCount)
                        .readAt(LocalDateTime.now())
                        .build();

        domainEventPublisher.publish(
                EventTypes.NOTIFICATION_READ,
                notificationReadRoutingKey,
                event
        );
    }

}
