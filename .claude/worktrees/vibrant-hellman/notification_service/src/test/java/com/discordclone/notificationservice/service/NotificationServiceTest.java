package com.discordclone.notificationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.discordclone.common.event.EventTypes;
import com.discordclone.common.event.MessageSentEvent;
import com.discordclone.notificationservice.entity.Notification;
import com.discordclone.notificationservice.entity.NotificationSettings;
import com.discordclone.notificationservice.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationServiceTest {

    private ProcessedEventService processedEventService;
    private NotificationRepository notificationRepository;
    private DomainEventPublisher domainEventPublisher;
    private PushNotificationService pushNotificationService;
    private NotificationSettingsService notificationSettingsService;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        processedEventService = org.mockito.Mockito.mock(ProcessedEventService.class);
        notificationRepository = org.mockito.Mockito.mock(NotificationRepository.class);
        domainEventPublisher = org.mockito.Mockito.mock(DomainEventPublisher.class);
        pushNotificationService = org.mockito.Mockito.mock(PushNotificationService.class);
        notificationSettingsService = org.mockito.Mockito.mock(NotificationSettingsService.class);
        when(notificationSettingsService.getSettings(any(UUID.class)))
                .thenAnswer(invocation -> NotificationSettings.builder()
                        .userId(invocation.getArgument(0))
                        .build());
        when(notificationSettingsService.getSettingsForUsers(any()))
                .thenAnswer(invocation -> ((java.util.Set<UUID>) invocation.getArgument(0)).stream()
                        .collect(Collectors.toMap(
                                userId -> userId,
                                userId -> NotificationSettings.builder().userId(userId).build()
                        )));
        when(processedEventService.tryMarkProcessing(any(UUID.class), anyString())).thenReturn(true);
        notificationService = new NotificationService(
                processedEventService,
                notificationRepository,
                domainEventPublisher,
                pushNotificationService,
                notificationSettingsService
        );
    }

    @Test
    void createFromMessageSentCreatesNotificationOnlyForMentionedRecipient() {
        UUID eventId = UUID.randomUUID();
        UUID senderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID mentionedId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID normalRecipientId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        MessageSentEvent event = MessageSentEvent.builder()
                .messageId("01HX0000000000000000000001")
                .channelId(7L)
                .senderId(senderId)
                .senderUsername("alice")
                .contentPreview("hello mention")
                .recipientIds(List.of(senderId, mentionedId, normalRecipientId))
                .mentionedUserIds(List.of(mentionedId))
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.createFromMessageSent(event, eventId);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = captor.getValue();

        assertThat(notifications).hasSize(1);
        assertThat(notifications)
                .filteredOn(notification -> notification.getRecipientId().equals(mentionedId))
                .singleElement()
                .satisfies(notification -> {
                    assertThat(notification.getType()).isEqualTo("MENTION");
                    assertThat(notification.getTitle()).isEqualTo("alice mentioned you");
                    assertThat(notification.getBody()).isEqualTo("hello mention");
                });
        assertThat(notifications)
                .filteredOn(notification -> notification.getRecipientId().equals(normalRecipientId))
                .isEmpty();
        verify(processedEventService).tryMarkProcessing(eventId, EventTypes.MESSAGE_SENT);
    }

    @Test
    void createFromMessageSentSkipsRegularChannelMessageWithoutMentions() {
        UUID eventId = UUID.randomUUID();
        UUID senderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID recipientId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        MessageSentEvent event = MessageSentEvent.builder()
                .messageId("01HX0000000000000000000001")
                .channelId(7L)
                .senderId(senderId)
                .senderUsername("alice")
                .contentPreview("regular message")
                .recipientIds(List.of(senderId, recipientId))
                .mentionedUserIds(List.of())
                .createdAt(LocalDateTime.now())
                .build();

        notificationService.createFromMessageSent(event, eventId);

        verify(notificationRepository, never()).saveAll(any());
        verify(domainEventPublisher, never()).publish(any(), any(), any());
        verify(processedEventService).tryMarkProcessing(eventId, EventTypes.MESSAGE_SENT);
    }

    @Test
    void createFromMessageSentUsesBroadcastMentionTitle() {
        UUID eventId = UUID.randomUUID();
        UUID senderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID recipientId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        MessageSentEvent event = MessageSentEvent.builder()
                .messageId("01HX0000000000000000000001")
                .senderId(senderId)
                .senderUsername("alice")
                .recipientIds(List.of(senderId, recipientId))
                .mentionedUserIds(List.of(recipientId))
                .mentionEveryone(true)
                .contentPreview("@everyone deploy time")
                .build();

        when(notificationRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.createFromMessageSent(event, eventId);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .singleElement()
                .satisfies(notification -> assertThat(notification.getTitle()).isEqualTo("alice mentioned everyone"));
    }

    @Test
    void createFromMessageSentSkipsDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        when(processedEventService.tryMarkProcessing(eventId, EventTypes.MESSAGE_SENT)).thenReturn(false);

        notificationService.createFromMessageSent(MessageSentEvent.builder().build(), eventId);

        verify(notificationRepository, never()).saveAll(any());
    }
}
