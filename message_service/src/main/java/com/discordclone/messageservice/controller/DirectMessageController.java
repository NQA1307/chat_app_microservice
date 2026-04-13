package com.discordclone.messageservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.messageservice.dto.ConversationResponse;
import com.discordclone.messageservice.dto.CreateConversationRequest;
import com.discordclone.messageservice.dto.DirectMessageResponse;
import com.discordclone.messageservice.dto.SendDirectMessageRequest;
import com.discordclone.messageservice.service.DirectMessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;

    /**
     * POST /api/dm/conversations
     * Tạo hoặc lấy conversation với targetUserId
     */
    @PostMapping("/api/dm/conversations")
    public ApiResponse<ConversationResponse> getOrCreateConversation(
            @RequestBody CreateConversationRequest req,
            HttpServletRequest request) {
        Long currentUserId = Long.parseLong(request.getHeader("X-User-Id"));
        return ApiResponse.success(
                directMessageService.getOrCreateConversation(currentUserId, req.getTargetUserId())
        );
    }

    /**
     * GET /api/dm/conversations
     * Danh sách conversations của user hiện tại
     */
    @GetMapping("/api/dm/conversations")
    public ApiResponse<List<ConversationResponse>> getConversations(HttpServletRequest request) {
        Long currentUserId = Long.parseLong(request.getHeader("X-User-Id"));
        return ApiResponse.success(directMessageService.getConversations(currentUserId));
    }

    /**
     * GET /api/dm/conversations/{conversationId}/messages?page=0&size=50
     * Lịch sử tin nhắn DM (có phân trang)
     */
    @GetMapping("/api/dm/conversations/{conversationId}/messages")
    public ApiResponse<List<DirectMessageResponse>> getDirectMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(directMessageService.getDirectMessages(conversationId, page, size));
    }

    /**
     * POST /api/dm/conversations/{conversationId}/messages
     * Gửi tin nhắn DM qua REST
     */
    @PostMapping("/api/dm/conversations/{conversationId}/messages")
    public ApiResponse<DirectMessageResponse> sendDirectMessage(
            @PathVariable Long conversationId,
            @RequestBody SendDirectMessageRequest req,
            HttpServletRequest request) {
        req.setConversationId(conversationId);
        req.setSenderId(Long.parseLong(request.getHeader("X-User-Id")));
        req.setSenderUsername(request.getHeader("X-User-Username"));
        return ApiResponse.success(directMessageService.sendDirectMessage(req));
    }

    /**
     * STOMP: Client gửi tới /app/dm.send
     * Server lưu DB và broadcast về /topic/dm/{conversationId}
     */
    @MessageMapping("/dm.send")
    public void sendViaWebSocket(@Payload SendDirectMessageRequest req) {
        directMessageService.sendDirectMessage(req);
    }
}
