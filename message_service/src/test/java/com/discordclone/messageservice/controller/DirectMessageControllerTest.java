package com.discordclone.messageservice.controller;

import com.discordclone.messageservice.dto.request.SendDirectMessageRequest;
import com.discordclone.messageservice.dto.response.ConversationResponse;
import com.discordclone.messageservice.dto.response.DirectMessageResponse;
import com.discordclone.messageservice.security.SecurityConfig;
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
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DirectMessageController.class)
@Import(SecurityConfig.class)
class DirectMessageControllerTest {

    private static final UUID USER_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final String DM_ID_1 = "01HX0000000000000000000001";
    private static final String DM_ID_2 = "01HX0000000000000000000002";
    private static final String DM_ID_3 = "01HX0000000000000000000003";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DirectMessageService directMessageService;

    @Test
    void postConversations_createsOrGetsConversation() throws Exception {
        ConversationResponse response = ConversationResponse.builder()
                .id(1L)
                .participantId1(USER_1)
                .participantId2(USER_2)
                .createdAt(LocalDateTime.now())
                .build();

        when(directMessageService.getOrCreateConversation(USER_1, USER_2)).thenReturn(response);

        mockMvc.perform(post("/api/dm/conversations")
                        .header("X-User-Id", USER_1.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("targetUserId", USER_2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.participantId1").value(USER_1.toString()))
                .andExpect(jsonPath("$.data.participantId2").value(USER_2.toString()));

        verify(directMessageService).getOrCreateConversation(USER_1, USER_2);
    }

    @Test
    void getConversations_returnsUserConversations() throws Exception {
        List<ConversationResponse> list = List.of(
                ConversationResponse.builder().id(1L).participantId1(USER_1).participantId2(USER_2)
                        .createdAt(LocalDateTime.now()).build(),
                ConversationResponse.builder().id(2L).participantId1(USER_1).participantId2(USER_3)
                        .createdAt(LocalDateTime.now()).build()
        );

        when(directMessageService.getConversations(USER_1)).thenReturn(list);

        mockMvc.perform(get("/api/dm/conversations")
                        .header("X-User-Id", USER_1.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2));
    }

    @Test
    void getConversations_noConversations_returnsEmptyList() throws Exception {
        when(directMessageService.getConversations(USER_3)).thenReturn(List.of());

        mockMvc.perform(get("/api/dm/conversations")
                        .header("X-User-Id", USER_3.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void getMessages_returnsPagedMessages() throws Exception {
        List<DirectMessageResponse> messages = List.of(
                DirectMessageResponse.builder().id(DM_ID_1).conversationId(1L).senderId(USER_2)
                        .senderUsername("bob").content("Hello!").createdAt(LocalDateTime.now()).build()
        );

        when(directMessageService.getDirectMessages(1L, USER_1, 0, 50)).thenReturn(messages);

        mockMvc.perform(get("/api/dm/conversations/1/messages")
                        .header("X-User-Id", USER_1.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(DM_ID_1))
                .andExpect(jsonPath("$.data[0].content").value("Hello!"))
                .andExpect(jsonPath("$.data[0].senderUsername").value("bob"));
    }

    @Test
    void getMessages_customPageAndSize() throws Exception {
        when(directMessageService.getDirectMessages(1L, USER_1, 2, 10)).thenReturn(List.of());

        mockMvc.perform(get("/api/dm/conversations/1/messages")
                        .header("X-User-Id", USER_1.toString())
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(directMessageService).getDirectMessages(1L, USER_1, 2, 10);
    }

    @Test
    void postMessage_sendsDirectMessage() throws Exception {
        DirectMessageResponse response = DirectMessageResponse.builder()
                .id(DM_ID_2).conversationId(3L).senderId(USER_1)
                .senderUsername("alice").content("Hi Bob")
                .createdAt(LocalDateTime.now()).build();

        when(directMessageService.sendDirectMessage(any(SendDirectMessageRequest.class), eq(USER_1), eq("alice")))
                .thenReturn(response);

        mockMvc.perform(post("/api/dm/conversations/3/messages")
                        .header("X-User-Id", USER_1.toString())
                        .header("X-User-Username", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Hi Bob"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(DM_ID_2))
                .andExpect(jsonPath("$.data.content").value("Hi Bob"))
                .andExpect(jsonPath("$.data.senderUsername").value("alice"));
    }

    @Test
    void postMessage_setsConversationIdAndSenderFromHeaders() throws Exception {
        DirectMessageResponse response = DirectMessageResponse.builder()
                .id(DM_ID_3).conversationId(7L).senderId(USER_3)
                .senderUsername("charlie").content("Hey")
                .createdAt(LocalDateTime.now()).build();

        when(directMessageService.sendDirectMessage(any(SendDirectMessageRequest.class), eq(USER_3), eq("charlie")))
                .thenReturn(response);

        mockMvc.perform(post("/api/dm/conversations/7/messages")
                        .header("X-User-Id", USER_3.toString())
                        .header("X-User-Username", "charlie")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Hey"))))
                .andExpect(status().isOk());

        verify(directMessageService).sendDirectMessage(argThat(req ->
                req.getConversationId().equals(7L)
                        && req.getContent().equals("Hey")
        ), eq(USER_3), eq("charlie"));
    }
}
