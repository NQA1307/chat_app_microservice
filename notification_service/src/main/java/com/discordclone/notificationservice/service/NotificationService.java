package com.discordclone.notificationservice.service;

import com.discordclone.common.event.DirectMessageSentEvent;
import com.discordclone.common.event.MessageSentEvent;
import com.discordclone.common.event.ServerInviteEvent;
import com.discordclone.common.exception.AppException;
import com.discordclone.notificationservice.dto.NotificationResponse;
import com.discordclone.notificationservice.entity.Notification;
import com.discordclone.notificationservice.repository.NotificationRepository;
import de.huxhorn.sulky.ulid.ULID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ULID ulid = new ULID();

    public List<NotificationResponse> getNotifications(UUID userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public long countUnread(UUID userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(String notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getRecipientId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You cannot access this notification");
        }

        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void createFromMessageSent(MessageSentEvent event) {
        if (event.getRecipientIds() == null || event.getRecipientIds().isEmpty()) {
            return;
        }

        List<Notification> notifications = event.getRecipientIds().stream()
                .filter(recipientId -> !recipientId.equals(event.getSenderId()))
                .map(recipientId -> Notification.builder()
                        .id(ulid.nextULID())
                        .recipientId(recipientId)
                        .type("MESSAGE_SENT")
                        .title(event.getSenderUsername() + " sent a message")
                        .body(event.getContent())
                        .read(false)
                        .sourceType("MESSAGE")
                        .sourceId(event.getMessageId())
                        .build())
                .toList();

        notificationRepository.saveAll(notifications);
    }

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
    public void createFromDirectMessageSent(DirectMessageSentEvent event) {
        if (event.receiverId().equals(event.senderId())) {
            return;
        }

        Notification notification = Notification.builder()
                .id(ulid.nextULID())
                .recipientId(event.receiverId())
                .type("DM_MESSAGE")
                .title("New direct message")
                .body(event.content())
                .read(false)
                .sourceType("DIRECT_MESSAGE")
                .sourceId(String.valueOf(event.conversationId()))
                .build();

        notificationRepository.save(notification);
    }

    //Tao noti tu event gui loi moi tham gia server
    @Transactional
    public void createFromServerInvite(ServerInviteEvent event) {
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
    }

    @Transactional
    public int markSourceAsRead(UUID userId, String sourceType, String sourceId) {
        return notificationRepository.markSourceAsRead(userId, sourceType, sourceId);
    }

}
