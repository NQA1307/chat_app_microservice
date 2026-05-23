package com.discordclone.notificationservice.repository;

import com.discordclone.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);

    long countByRecipientIdAndReadFalse(UUID recipientId);

    List<Notification> findByRecipientIdAndSourceTypeAndSourceIdAndReadFalse(
            UUID recipientId,
            String sourceType,
            String sourceId
    );

    @Modifying
    @Query("""
            update Notification notification
            set notification.read = true
            where notification.recipientId = :recipientId
              and upper(notification.sourceType) = upper(:sourceType)
              and notification.sourceId = :sourceId
              and notification.read = false
            """)
    int markSourceAsRead(
            @Param("recipientId") UUID recipientId,
            @Param("sourceType") String sourceType,
            @Param("sourceId") String sourceId
    );
}
