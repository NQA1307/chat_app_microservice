package com.discordclone.messageservice.service;

import com.discordclone.messageservice.dto.ConversationResponse;
import com.discordclone.messageservice.dto.DirectMessageResponse;
import com.discordclone.messageservice.dto.SendDirectMessageRequest;
import com.discordclone.messageservice.entity.Conversation;
import com.discordclone.messageservice.entity.DirectMessage;
import com.discordclone.messageservice.repository.ConversationRepository;
import com.discordclone.messageservice.repository.DirectMessageRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DirectMessageServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private DirectMessageService directMessageService;

    // ── getOrCreateConversation ────────────────────────────────

    @Test
    void getOrCreateConversation_existingConversation_returnsExisting() {
        Conversation existing = Conversation.builder()
                .id(1L).participantId1(2L).participantId2(5L)
                .createdAt(LocalDateTime.now()).build();

        when(conversationRepository.findByParticipantId1AndParticipantId2(2L, 5L))
                .thenReturn(Optional.of(existing));

        ConversationResponse result = directMessageService.getOrCreateConversation(5L, 2L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getParticipantId1()).isEqualTo(2L);
        assertThat(result.getParticipantId2()).isEqualTo(5L);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void getOrCreateConversation_noExisting_createsNew() {
        Conversation saved = Conversation.builder()
                .id(10L).participantId1(1L).participantId2(3L)
                .createdAt(LocalDateTime.now()).build();

        when(conversationRepository.findByParticipantId1AndParticipantId2(1L, 3L))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(saved);

        ConversationResponse result = directMessageService.getOrCreateConversation(3L, 1L);

        assertThat(result.getId()).isEqualTo(10L);
        // Kiểm tra sort đúng: id nhỏ hơn vào participantId1
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipantId1()).isEqualTo(1L);
        assertThat(captor.getValue().getParticipantId2()).isEqualTo(3L);
    }

    @Test
    void getOrCreateConversation_sortIds_alwaysStoresSmallerId1() {
        Conversation saved = Conversation.builder()
                .id(1L).participantId1(2L).participantId2(7L)
                .createdAt(LocalDateTime.now()).build();

        when(conversationRepository.findByParticipantId1AndParticipantId2(2L, 7L))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any())).thenReturn(saved);

        // Gọi với thứ tự ngược: currentUser=7, target=2
        directMessageService.getOrCreateConversation(7L, 2L);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipantId1()).isEqualTo(2L);
        assertThat(captor.getValue().getParticipantId2()).isEqualTo(7L);
    }

    // ── getConversations ──────────────────────────────────────

    @Test
    void getConversations_returnsAllConversationsForUser() {
        List<Conversation> conversations = List.of(
                Conversation.builder().id(1L).participantId1(1L).participantId2(2L).createdAt(LocalDateTime.now()).build(),
                Conversation.builder().id(2L).participantId1(1L).participantId2(3L).createdAt(LocalDateTime.now()).build()
        );

        when(conversationRepository.findByParticipantId1OrParticipantId2(1L, 1L))
                .thenReturn(conversations);

        List<ConversationResponse> result = directMessageService.getConversations(1L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ConversationResponse::getId).containsExactly(1L, 2L);
    }

    @Test
    void getConversations_noConversations_returnsEmptyList() {
        when(conversationRepository.findByParticipantId1OrParticipantId2(99L, 99L))
                .thenReturn(List.of());

        List<ConversationResponse> result = directMessageService.getConversations(99L);

        assertThat(result).isEmpty();
    }

    // ── sendDirectMessage ─────────────────────────────────────

    @Test
    void sendDirectMessage_savesAndBroadcasts() {
        SendDirectMessageRequest req = new SendDirectMessageRequest();
        req.setConversationId(5L);
        req.setSenderId(1L);
        req.setSenderUsername("alice");
        req.setContent("Hello Bob!");

        DirectMessage saved = DirectMessage.builder()
                .id(100L).conversationId(5L).senderId(1L)
                .senderUsername("alice").content("Hello Bob!")
                .createdAt(LocalDateTime.now()).build();

        when(directMessageRepository.save(any(DirectMessage.class))).thenReturn(saved);

        DirectMessageResponse result = directMessageService.sendDirectMessage(req);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getContent()).isEqualTo("Hello Bob!");
        assertThat(result.getSenderUsername()).isEqualTo("alice");

        verify(messagingTemplate).convertAndSend(eq("/topic/dm/5"), any(DirectMessageResponse.class));
    }

    @Test
    void sendDirectMessage_broadcastsToCorrectTopic() {
        SendDirectMessageRequest req = new SendDirectMessageRequest();
        req.setConversationId(42L);
        req.setSenderId(1L);
        req.setSenderUsername("user1");
        req.setContent("Hi");

        DirectMessage saved = DirectMessage.builder()
                .id(1L).conversationId(42L).senderId(1L)
                .senderUsername("user1").content("Hi")
                .createdAt(LocalDateTime.now()).build();

        when(directMessageRepository.save(any())).thenReturn(saved);

        directMessageService.sendDirectMessage(req);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSend(topicCaptor.capture(), any(DirectMessageResponse.class));
        assertThat(topicCaptor.getValue()).isEqualTo("/topic/dm/42");
    }

    // ── getDirectMessages ─────────────────────────────────────

    @Test
    void getDirectMessages_returnsPaginatedMessages() {
        List<DirectMessage> messages = List.of(
                DirectMessage.builder().id(3L).conversationId(1L).senderId(2L)
                        .senderUsername("bob").content("Hey").createdAt(LocalDateTime.now()).build(),
                DirectMessage.builder().id(2L).conversationId(1L).senderId(1L)
                        .senderUsername("alice").content("Hello").createdAt(LocalDateTime.now()).build()
        );

        when(directMessageRepository.findByConversationIdOrderByCreatedAtDesc(
                eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(messages));

        List<DirectMessageResponse> result = directMessageService.getDirectMessages(1L, 0, 50);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(3L);
        assertThat(result.get(1).getContent()).isEqualTo("Hello");
    }

    @Test
    void getDirectMessages_emptyConversation_returnsEmptyList() {
        when(directMessageRepository.findByConversationIdOrderByCreatedAtDesc(
                eq(999L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<DirectMessageResponse> result = directMessageService.getDirectMessages(999L, 0, 50);

        assertThat(result).isEmpty();
    }
}
