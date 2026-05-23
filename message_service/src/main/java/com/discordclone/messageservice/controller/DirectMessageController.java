package com.discordclone.messageservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.common.exception.AppException;
import com.discordclone.messageservice.dto.request.CreateConversationRequest;
import com.discordclone.messageservice.dto.request.SendDirectMessageRequest;
import com.discordclone.messageservice.dto.request.UpdateMessageRequest;
import com.discordclone.messageservice.dto.response.ConversationResponse;
import com.discordclone.messageservice.dto.response.DirectMessageResponse;
import com.discordclone.messageservice.service.DirectMessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
            @Valid @RequestBody CreateConversationRequest req,
            HttpServletRequest request) {
        UUID currentUserId = UUID.fromString(request.getHeader("X-User-Id"));
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
        UUID currentUserId = UUID.fromString(request.getHeader("X-User-Id"));
        return ApiResponse.success(directMessageService.getConversations(currentUserId));
    }

    /**
     * GET /api/dm/conversations/{conversationId}/messages?page=0&size=50
     * Lịch sử tin nhắn DM (có phân trang)
     */
    @GetMapping("/api/dm/conversations/{conversationId}/messages")
    public ApiResponse<List<DirectMessageResponse>> getDirectMessages(
            @PathVariable("conversationId") Long conversationId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            HttpServletRequest request) {
        UUID currentUserId = UUID.fromString(request.getHeader("X-User-Id"));
        return ApiResponse.success(directMessageService.getDirectMessages(conversationId, currentUserId, page, size));
    }

    /**
     * POST /api/dm/conversations/{conversationId}/messages
     * Gửi tin nhắn DM qua REST
     */
    @PostMapping("/api/dm/conversations/{conversationId}/messages")
    public ApiResponse<DirectMessageResponse> sendDirectMessage(
            @PathVariable("conversationId") Long conversationId,
            @Valid @RequestBody SendDirectMessageRequest req,
            HttpServletRequest request) {
        UUID currentUserId = UUID.fromString(request.getHeader("X-User-Id"));
        String username = request.getHeader("X-User-Username");
        req.setConversationId(conversationId);
        return ApiResponse.success(directMessageService.sendDirectMessage(req, currentUserId, username));
    }

    //Cap nhat tin nhan
    @PatchMapping("/api/dm/messages/{messageId}")
    public ApiResponse<DirectMessageResponse> updateDirectMessage(
        @PathVariable("messageId") String messageId,
        @Valid @RequestBody UpdateMessageRequest req,
        HttpServletRequest request) {
    UUID currentUserId = getUserId(request);
    return ApiResponse.success(
            directMessageService.updateDirectMessage(messageId, currentUserId, req)
    );
    }


    //Xoa tin nhan
    @DeleteMapping("/api/dm/messages/{messageId}")
    public ApiResponse<DirectMessageResponse> deleteDirectMessage(
        @PathVariable("messageId") String messageId,
        HttpServletRequest request) {
    UUID currentUserId = getUserId(request);
    return ApiResponse.success(
            directMessageService.deleteDirectMessage(messageId, currentUserId)
    );
    }

    private UUID getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Missing authenticated user headers");
        }
        return UUID.fromString(userId);
    }

}
