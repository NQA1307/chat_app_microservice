package com.discordclone.messageservice.service;

import com.discordclone.common.exception.AppException;
import com.discordclone.messageservice.dto.request.SendDirectMessageRequest;
import com.discordclone.messageservice.dto.response.ConversationResponse;
import com.discordclone.messageservice.dto.response.DirectMessageResponse;
import com.discordclone.messageservice.entity.Conversation;
import com.discordclone.messageservice.entity.DirectMessage;
import com.discordclone.messageservice.entity.ReactionSourceType;
import com.discordclone.messageservice.client.UserServiceClient;
import com.discordclone.messageservice.repository.ConversationRepository;
import com.discordclone.messageservice.repository.DirectMessageRepository;
import com.discordclone.messageservice.repository.MessageMentionRepository;
import com.discordclone.messageservice.security.MessageContentSanitizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
class DirectMessageServiceTest {

    private static final UUID USER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final String DM_ID_1 = "01HX0000000000000000000001";
    private static final String DM_ID_2 = "01HX0000000000000000000002";
    private static final String DM_ID_3 = "01HX0000000000000000000003";

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessageEventPublisher messageEventPublisher;

    @Mock
    private MessageReactionService messageReactionService;

    @Mock
    private MessageContentSanitizer messageContentSanitizer;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private MentionParser mentionParser;

    @Mock
    private MessageMentionRepository mentionRepository;

    @Mock
    private TypingIndicatorService typingIndicatorService;

    @InjectMocks
    private DirectMessageService directMessageService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(messageContentSanitizer.sanitizePlainText(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(mentionParser.parse(any())).thenReturn(new MentionParseResult(List.of(), false, false));
        lenient().when(messageReactionService.getSummary(any(ReactionSourceType.class), any(String.class))).thenReturn(List.of());
        lenient().when(messageReactionService.getSummaryMap(any(ReactionSourceType.class), any())).thenReturn(java.util.Map.of());
        lenient().when(userServiceClient.checkBlockStatus(any(UUID.class), any(UUID.class)))
                .thenReturn(new UserServiceClient.BlockStatusResponse(false, null));
    }

    @Test
    void getOrCreateConversation_existingConversation_returnsExisting() {
        Conversation existing = Conversation.builder()
                .id(1L).participantId1(USER_1).participantId2(USER_2)
                .createdAt(LocalDateTime.now()).build();

        when(conversationRepository.findByParticipantId1AndParticipantId2(USER_1, USER_2))
                .thenReturn(Optional.of(existing));

        ConversationResponse result = directMessageService.getOrCreateConversation(USER_2, USER_1);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getParticipantId1()).isEqualTo(USER_1);
        assertThat(result.getParticipantId2()).isEqualTo(USER_2);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void getOrCreateConversation_noExisting_createsNew() {
        Conversation saved = Conversation.builder()
                .id(10L).participantId1(USER_1).participantId2(USER_3)
                .createdAt(LocalDateTime.now()).build();

        when(conversationRepository.findByParticipantId1AndParticipantId2(USER_1, USER_3))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(saved);

        ConversationResponse result = directMessageService.getOrCreateConversation(USER_3, USER_1);

        assertThat(result.getId()).isEqualTo(10L);
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipantId1()).isEqualTo(USER_1);
        assertThat(captor.getValue().getParticipantId2()).isEqualTo(USER_3);
    }

    @Test
    void getOrCreateConversation_sameUser_throwsBadRequest() {
        assertThatThrownBy(() -> directMessageService.getOrCreateConversation(USER_1, USER_1))
                .isInstanceOf(AppException.class);
    }

    @Test
    void getConversations_returnsAllConversationsForUser() {
        List<Conversation> conversations = List.of(
                Conversation.builder().id(1L).participantId1(USER_1).participantId2(USER_2).createdAt(LocalDateTime.now()).build(),
                Conversation.builder().id(2L).participantId1(USER_1).participantId2(USER_3).createdAt(LocalDateTime.now()).build()
        );

        when(conversationRepository.findByParticipantId1OrParticipantId2(USER_1, USER_1))
                .thenReturn(conversations);

        List<ConversationResponse> result = directMessageService.getConversations(USER_1);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ConversationResponse::getId).containsExactly(1L, 2L);
    }

    @Test
    void getConversations_noConversations_returnsEmptyList() {
        when(conversationRepository.findByParticipantId1OrParticipantId2(USER_3, USER_3))
                .thenReturn(List.of());

        List<ConversationResponse> result = directMessageService.getConversations(USER_3);

        assertThat(result).isEmpty();
    }

    @Test
    void sendDirectMessage_savesAndBroadcasts() {
        SendDirectMessageRequest req = new SendDirectMessageRequest();
        req.setConversationId(5L);
        req.setContent("Hello Bob!");

        Conversation conversation = Conversation.builder()
                .id(5L).participantId1(USER_1).participantId2(USER_2).build();
        DirectMessage saved = DirectMessage.builder()
                .id(DM_ID_1).conversationId(5L).senderId(USER_1)
                .senderUsername("alice").content("Hello Bob!")
                .createdAt(LocalDateTime.now()).build();

        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conversation));
        when(directMessageRepository.save(any(DirectMessage.class))).thenReturn(saved);

        DirectMessageResponse result = directMessageService.sendDirectMessage(req, USER_1, "alice");

        assertThat(result.getId()).isEqualTo(DM_ID_1);
        assertThat(result.getContent()).isEqualTo("Hello Bob!");
        assertThat(result.getSenderId()).isEqualTo(USER_1);
        assertThat(result.getSenderUsername()).isEqualTo("alice");

        verify(messagingTemplate).convertAndSend(eq("/topic/dm.5"), any(DirectMessageResponse.class));
    }

    @Test
    void sendDirectMessage_rejectsNonParticipant() {
        SendDirectMessageRequest req = new SendDirectMessageRequest();
        req.setConversationId(42L);
        req.setContent("Hi");

        Conversation conversation = Conversation.builder()
                .id(42L).participantId1(USER_1).participantId2(USER_2).build();

        when(conversationRepository.findById(42L)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> directMessageService.sendDirectMessage(req, USER_3, "charlie"))
                .isInstanceOf(AppException.class);
    }

    @Test
    void sendDirectMessage_attachmentOnly_savesAndBroadcasts() {
        SendDirectMessageRequest req = new SendDirectMessageRequest();
        req.setConversationId(5L);
        req.setContent("");
        req.setMessageType("FILE");
        req.setFileUrl("http://example.com/file.png");
        req.setFileName("file.png");
        req.setFileSize(100L);
        req.setContentType("image/png");

        Conversation conversation = Conversation.builder()
                .id(5L).participantId1(USER_1).participantId2(USER_2).build();
        DirectMessage saved = DirectMessage.builder()
                .id(DM_ID_1).conversationId(5L).senderId(USER_1)
                .senderUsername("alice").content("")
                .fileUrl("http://example.com/file.png")
                .fileName("file.png")
                .fileSize(100L)
                .contentType("image/png")
                .createdAt(LocalDateTime.now()).build();

        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conversation));
        when(directMessageRepository.save(any(DirectMessage.class))).thenReturn(saved);

        DirectMessageResponse result = directMessageService.sendDirectMessage(req, USER_1, "alice");

        assertThat(result.getId()).isEqualTo(DM_ID_1);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getFileUrl()).isEqualTo("http://example.com/file.png");
        assertThat(result.getFileName()).isEqualTo("file.png");
        assertThat(result.getFileSize()).isEqualTo(100L);
        assertThat(result.getContentType()).isEqualTo("image/png");

        verify(messagingTemplate).convertAndSend(eq("/topic/dm.5"), any(DirectMessageResponse.class));
    }

    @Test
    void sendDirectMessage_missingAttachmentMetadata_throwsBadRequest() {
        SendDirectMessageRequest req = new SendDirectMessageRequest();
        req.setConversationId(5L);
        req.setContent("");
        req.setMessageType("FILE");
        req.setFileUrl("http://example.com/file.png");

        Conversation conversation = Conversation.builder()
                .id(5L).participantId1(USER_1).participantId2(USER_2).build();

        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> directMessageService.sendDirectMessage(req, USER_1, "alice"))
                .isInstanceOf(AppException.class);
    }

    @Test
    void getDirectMessages_returnsPaginatedMessages() {
        Conversation conversation = Conversation.builder()
                .id(1L).participantId1(USER_1).participantId2(USER_2).build();
        List<DirectMessage> messages = List.of(
                DirectMessage.builder().id(DM_ID_2).conversationId(1L).senderId(USER_2)
                        .senderUsername("bob").content("Hey").createdAt(LocalDateTime.now()).build(),
                DirectMessage.builder().id(DM_ID_3).conversationId(1L).senderId(USER_1)
                        .senderUsername("alice").content("Hello").createdAt(LocalDateTime.now()).build()
        );

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(directMessageRepository.findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(
                eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(messages));

        List<DirectMessageResponse> result = directMessageService.getDirectMessages(1L, USER_1, 0, 50);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(DM_ID_2);
        assertThat(result.get(1).getContent()).isEqualTo("Hello");
    }

    @Test
    void getDirectMessages_emptyConversation_returnsEmptyList() {
        Conversation conversation = Conversation.builder()
                .id(999L).participantId1(USER_1).participantId2(USER_2).build();

        when(conversationRepository.findById(999L)).thenReturn(Optional.of(conversation));
        when(directMessageRepository.findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(
                eq(999L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<DirectMessageResponse> result = directMessageService.getDirectMessages(999L, USER_1, 0, 50);

        assertThat(result).isEmpty();
    }
}
