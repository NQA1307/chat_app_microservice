package com.discordclone.messageservice.controller;

import com.discordclone.messageservice.dto.ConversationResponse;
import com.discordclone.messageservice.dto.DirectMessageResponse;
import com.discordclone.messageservice.service.DirectMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.discordclone.messageservice.security.SecurityConfig;

@WebMvcTest(DirectMessageController.class)
@Import(SecurityConfig.class)
class DirectMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DirectMessageService directMessageService;

    // ── POST /api/dm/conversations ────────────────────────────

    @Test
    void postConversations_createsOrGetsConversation() throws Exception {
        ConversationResponse response = ConversationResponse.builder()
                .id(1L).participantId1(2L).participantId2(5L)
                .createdAt(LocalDateTime.now()).build();

        when(directMessageService.getOrCreateConversation(5L, 2L)).thenReturn(response);

        mockMvc.perform(post("/api/dm/conversations")
                        .header("X-User-Id", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("targetUserId", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.participantId1").value(2))
                .andExpect(jsonPath("$.data.participantId2").value(5));

        verify(directMessageService).getOrCreateConversation(5L, 2L);
    }

    // ── GET /api/dm/conversations ─────────────────────────────

    @Test
    void getConversations_returnsUserConversations() throws Exception {
        List<ConversationResponse> list = List.of(
                ConversationResponse.builder().id(1L).participantId1(1L).participantId2(2L)
                        .createdAt(LocalDateTime.now()).build(),
                ConversationResponse.builder().id(2L).participantId1(1L).participantId2(3L)
                        .createdAt(LocalDateTime.now()).build()
        );

        when(directMessageService.getConversations(1L)).thenReturn(list);

        mockMvc.perform(get("/api/dm/conversations")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2));
    }

    @Test
    void getConversations_noConversations_returnsEmptyList() throws Exception {
        when(directMessageService.getConversations(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/dm/conversations")
                        .header("X-User-Id", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ── GET /api/dm/conversations/{id}/messages ───────────────

    @Test
    void getMessages_returnsPagedMessages() throws Exception {
        List<DirectMessageResponse> messages = List.of(
                DirectMessageResponse.builder().id(10L).conversationId(1L).senderId(2L)
                        .senderUsername("bob").content("Hello!").createdAt(LocalDateTime.now()).build()
        );

        when(directMessageService.getDirectMessages(1L, 0, 50)).thenReturn(messages);

        mockMvc.perform(get("/api/dm/conversations/1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].content").value("Hello!"))
                .andExpect(jsonPath("$.data[0].senderUsername").value("bob"));
    }

    @Test
    void getMessages_customPageAndSize() throws Exception {
        when(directMessageService.getDirectMessages(1L, 2, 10)).thenReturn(List.of());

        mockMvc.perform(get("/api/dm/conversations/1/messages")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(directMessageService).getDirectMessages(1L, 2, 10);
    }

    // ── POST /api/dm/conversations/{id}/messages ──────────────

    @Test
    void postMessage_sendsDirectMessage() throws Exception {
        DirectMessageResponse response = DirectMessageResponse.builder()
                .id(50L).conversationId(3L).senderId(1L)
                .senderUsername("alice").content("Hi Bob")
                .createdAt(LocalDateTime.now()).build();

        when(directMessageService.sendDirectMessage(any())).thenReturn(response);

        mockMvc.perform(post("/api/dm/conversations/3/messages")
                        .header("X-User-Id", "1")
                        .header("X-User-Username", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Hi Bob"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(50))
                .andExpect(jsonPath("$.data.content").value("Hi Bob"))
                .andExpect(jsonPath("$.data.senderUsername").value("alice"));
    }

    @Test
    void postMessage_setsConversationIdAndSenderFromHeaders() throws Exception {
        DirectMessageResponse response = DirectMessageResponse.builder()
                .id(1L).conversationId(7L).senderId(3L)
                .senderUsername("charlie").content("Hey")
                .createdAt(LocalDateTime.now()).build();

        when(directMessageService.sendDirectMessage(any())).thenReturn(response);

        mockMvc.perform(post("/api/dm/conversations/7/messages")
                        .header("X-User-Id", "3")
                        .header("X-User-Username", "charlie")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Hey"))))
                .andExpect(status().isOk());

        // Verify service nhận đúng conversationId và sender từ path/headers
        verify(directMessageService).sendDirectMessage(argThat(req ->
                req.getConversationId().equals(7L)
                && req.getSenderId().equals(3L)
                && req.getSenderUsername().equals("charlie")
                && req.getContent().equals("Hey")
        ));
    }
}
