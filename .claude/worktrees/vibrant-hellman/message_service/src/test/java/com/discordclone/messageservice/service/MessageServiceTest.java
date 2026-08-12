package com.discordclone.messageservice.service;

import com.discordclone.common.exception.AppException;
import com.discordclone.common.event.MessageSentEvent;
import com.discordclone.messageservice.dto.request.SendMessageRequest;
import com.discordclone.messageservice.dto.request.UpdateMessageRequest;
import com.discordclone.messageservice.dto.response.MessageResponse;
import com.discordclone.messageservice.entity.Message;
import com.discordclone.messageservice.entity.ReactionSourceType;
import com.discordclone.messageservice.repository.ChannelReadStateRepository;
import com.discordclone.messageservice.repository.MessageMentionRepository;
import com.discordclone.messageservice.repository.MessageRepository;
import com.discordclone.messageservice.client.ServerServiceClient;
import com.discordclone.messageservice.security.MessageContentSanitizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    private static final Long CHANNEL_ID = 7L;
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private PermissionService permissionService;

    @Mock
    private MessageEventPublisher messageEventPublisher;

    @Mock
    private MessageReactionService messageReactionService;

    @Mock
    private ChannelReadStateRepository channelReadStateRepository;

    @Mock
    private MessageContentSanitizer messageContentSanitizer;

    @Mock
    private MentionParser mentionParser;

    @Mock
    private MessageMentionRepository mentionRepository;

    @Mock
    private ServerServiceClient serverServiceClient;

    @Mock
    private PresenceService presenceService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private TypingIndicatorService typingIndicatorService;

    @InjectMocks
    private MessageService messageService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(messageContentSanitizer.sanitizePlainText(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(mentionParser.parse(any())).thenReturn(new MentionParseResult(List.of(), false, false));
        lenient().when(messageReactionService.getSummary(any(ReactionSourceType.class), any(String.class))).thenReturn(List.of());
    }

    @Test
    void sendMessage_whenUserCannotAccessChannel_throwsForbiddenAndDoesNotSave() {
        SendMessageRequest request = validSendRequest();
        when(permissionService.canSendMessage(CHANNEL_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
        verify(messageEventPublisher, never()).publishMessageSent(any(MessageResponse.class));
    }

    @Test
    void sendMessage_whenUserCanAccessChannel_savesAndBroadcastsMessage() {
        SendMessageRequest request = validSendRequest();
        Message savedMessage = Message.builder()
                .id("01HX0000000000000000000001")
                .channelId(CHANNEL_ID)
                .senderId(USER_ID)
                .senderUsername("alice")
                .content("hello")
                .createdAt(LocalDateTime.now())
                .build();

        when(permissionService.canSendMessage(CHANNEL_ID, USER_ID)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        MessageResponse response = messageService.sendMessage(request);

        assertThat(response.getId()).isEqualTo(savedMessage.getId());
        assertThat(response.getChannelId()).isEqualTo(CHANNEL_ID);
        assertThat(response.getSenderId()).isEqualTo(USER_ID);
        assertThat(response.getContent()).isEqualTo("hello");

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("hello");

        verify(messagingTemplate).convertAndSend(eq("/topic/channel." + CHANNEL_ID), eq(response));
        verify(messageEventPublisher).publishMessageSent(any(MessageSentEvent.class));
    }

    @Test
    void getMessages_whenUserCannotAccessChannel_throwsForbiddenAndDoesNotReadMessages() {
        when(permissionService.canAccessChannel(CHANNEL_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> messageService.getMessages(CHANNEL_ID, USER_ID, 0, 50))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(messageRepository, never()).findByChannelIdAndDeletedFalseOrderByCreatedAtDesc(any(Long.class), any(PageRequest.class));
    }

    @Test
    void getMessages_whenUserCanAccessChannel_returnsMessages() {
        Message message = Message.builder()
                .id("01HX0000000000000000000002")
                .channelId(CHANNEL_ID)
                .senderId(USER_ID)
                .senderUsername("alice")
                .content("history")
                .createdAt(LocalDateTime.now())
                .build();

        when(permissionService.canAccessChannel(CHANNEL_ID, USER_ID)).thenReturn(true);
        when(messageRepository.findByChannelIdAndDeletedFalseOrderByCreatedAtDesc(eq(CHANNEL_ID), eq(PageRequest.of(0, 50))))
                .thenReturn(new PageImpl<>(List.of(message)));

        List<MessageResponse> responses = messageService.getMessages(CHANNEL_ID, USER_ID, 0, 50);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(message.getId());
        assertThat(responses.get(0).getContent()).isEqualTo("history");
    }

    @Test
    void updateMessage_whenOwnerCanAccessChannel_updatesAndBroadcastsMessage() {
        UpdateMessageRequest request = new UpdateMessageRequest();
        request.setContent(" updated content ");
        Message message = existingMessage("old content", false, USER_ID);
        Message savedMessage = existingMessage("updated content", false, USER_ID);

        when(messageRepository.findById(message.getId())).thenReturn(java.util.Optional.of(message));
        when(permissionService.canAccessChannel(CHANNEL_ID, USER_ID)).thenReturn(true);
        when(messageRepository.save(message)).thenReturn(savedMessage);

        MessageResponse response = messageService.updateMessage(message.getId(), USER_ID, request);

        assertThat(response.getContent()).isEqualTo("updated content");
        verify(messageRepository).save(message);
        verify(messagingTemplate).convertAndSend(eq("/topic/channel." + CHANNEL_ID), eq(response));
    }

    @Test
    void updateMessage_whenUserIsNotOwner_throwsForbidden() {
        UpdateMessageRequest request = new UpdateMessageRequest();
        request.setContent("updated content");
        Message message = existingMessage("old content", false, OTHER_USER_ID);

        when(messageRepository.findById(message.getId())).thenReturn(java.util.Optional.of(message));
        when(permissionService.canAccessChannel(CHANNEL_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> messageService.updateMessage(message.getId(), USER_ID, request))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void deleteMessage_whenOwnerCanAccessChannel_softDeletesAndBroadcastsMessage() {
        Message message = existingMessage("content", false, USER_ID);
        Message savedMessage = existingMessage("", true, USER_ID);

        when(messageRepository.findById(message.getId())).thenReturn(java.util.Optional.of(message));
        when(permissionService.canAccessChannel(CHANNEL_ID, USER_ID)).thenReturn(true);
        when(messageRepository.save(message)).thenReturn(savedMessage);

        MessageResponse response = messageService.deleteMessage(message.getId(), USER_ID);

        assertThat(response.isDeleted()).isTrue();
        assertThat(response.getContent()).isEmpty();
        verify(messageRepository).save(message);
        verify(messagingTemplate).convertAndSend(eq("/topic/channel." + CHANNEL_ID), eq(response));
    }

    @Test
    void deleteMessage_whenUserCannotAccessChannel_throwsForbidden() {
        Message message = existingMessage("content", false, USER_ID);

        when(messageRepository.findById(message.getId())).thenReturn(java.util.Optional.of(message));
        when(permissionService.canAccessChannel(CHANNEL_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> messageService.deleteMessage(message.getId(), USER_ID))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(messageRepository, never()).save(any(Message.class));
    }

    private SendMessageRequest validSendRequest() {
        SendMessageRequest request = new SendMessageRequest();
        request.setChannelId(CHANNEL_ID);
        request.setSenderId(USER_ID);
        request.setSenderUsername("alice");
        request.setContent("hello");
        return request;
    }

    @Test
    void sendMessage_attachmentOnly_savesAndBroadcastsMessage() {
        SendMessageRequest request = new SendMessageRequest();
        request.setChannelId(CHANNEL_ID);
        request.setSenderId(USER_ID);
        request.setSenderUsername("alice");
        request.setContent("");
        request.setMessageType("FILE");
        request.setFileUrl("http://example.com/file.png");
        request.setFileName("file.png");
        request.setFileSize(100L);
        request.setContentType("image/png");

        Message savedMessage = Message.builder()
                .id("01HX0000000000000000000001")
                .channelId(CHANNEL_ID)
                .senderId(USER_ID)
                .senderUsername("alice")
                .content("")
                .fileUrl("http://example.com/file.png")
                .fileName("file.png")
                .fileSize(100L)
                .contentType("image/png")
                .createdAt(LocalDateTime.now())
                .build();

        when(permissionService.canSendMessage(CHANNEL_ID, USER_ID)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        MessageResponse response = messageService.sendMessage(request);

        assertThat(response.getId()).isEqualTo(savedMessage.getId());
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getFileUrl()).isEqualTo("http://example.com/file.png");
        assertThat(response.getFileName()).isEqualTo("file.png");
        assertThat(response.getFileSize()).isEqualTo(100L);
        assertThat(response.getContentType()).isEqualTo("image/png");
    }

    @Test
    void sendMessage_missingAttachmentMetadata_throwsBadRequest() {
        SendMessageRequest request = new SendMessageRequest();
        request.setChannelId(CHANNEL_ID);
        request.setSenderId(USER_ID);
        request.setSenderUsername("alice");
        request.setContent("");
        request.setMessageType("FILE");
        request.setFileUrl("http://example.com/file.png");

        when(permissionService.canSendMessage(CHANNEL_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(AppException.class)
                .satisfies(error -> assertThat(((AppException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private Message existingMessage(String content, boolean deleted, UUID senderId) {
        return Message.builder()
                .id("01HX0000000000000000000003")
                .channelId(CHANNEL_ID)
                .senderId(senderId)
                .senderUsername("alice")
                .content(content)
                .deleted(deleted)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
