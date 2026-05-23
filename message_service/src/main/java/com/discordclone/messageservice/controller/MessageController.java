package com.discordclone.messageservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.common.exception.AppException;
import com.discordclone.messageservice.dto.request.MarkChannelReadRequest;
import com.discordclone.messageservice.dto.request.SendMessageRequest;
import com.discordclone.messageservice.dto.request.UpdateMessageRequest;
import com.discordclone.messageservice.dto.response.ChannelUnreadStateResponse;
import com.discordclone.messageservice.dto.response.MessageResponse;
import com.discordclone.messageservice.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @MessageMapping("/chat.send")
    public void sendViaWebSocket(@Payload SendMessageRequest req) {
        messageService.sendMessage(req);
    }

    // REST path is the trusted path: API Gateway forwards authenticated user headers.
    @PostMapping("/api/messages")
    public ApiResponse<MessageResponse> sendViaRest(
            @Valid @RequestBody SendMessageRequest req,
            HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-User-Username");

        if (userId == null || username == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Missing authenticated user headers");
        }

        req.setSenderId(UUID.fromString(userId));
        req.setSenderUsername(username);

        return ApiResponse.success(messageService.sendMessage(req));
    }

    // Load message history for the current authenticated user only.
    @GetMapping("/api/messages/{channelId}")
    public ApiResponse<List<MessageResponse>> getMessages(
            @PathVariable("channelId") Long channelId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Missing authenticated user headers");
        }

        return ApiResponse.success(messageService.getMessages(channelId, UUID.fromString(userId), page, size));
    }

    //Update noi dung tin nhan
    @PatchMapping("/api/messages/{messageId}")
    public ApiResponse<MessageResponse> updateMessage(
            @PathVariable("messageId") String messageId,
            @Valid @RequestBody UpdateMessageRequest req,
            HttpServletRequest request) {

        UUID userId = getUserId(request);
        return ApiResponse.success(messageService.updateMessage(messageId, userId, req));
    }

    //Xoa tin nhan
    @DeleteMapping("/api/messages/{messageId}")
    public ApiResponse<MessageResponse> deleteMessage(
            @PathVariable("messageId") String messageId,
            HttpServletRequest request) {

        UUID userId = getUserId(request);
        return ApiResponse.success(messageService.deleteMessage(messageId, userId));
    }

    private UUID getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Missing authenticated user headers");
        }
        return UUID.fromString(userId);
    }

    @PatchMapping("/api/messages/channels/{channelId}/read")
    public ApiResponse<Void> markChannelAsRead(
        @PathVariable("channelId") Long channelId,
        @RequestBody(required = false) MarkChannelReadRequest req,
        HttpServletRequest request) {
    UUID userId = getUserId(request);
    String messageId = req == null ? null : req.getMessageId();

    messageService.markChannelAsRead(channelId, userId, messageId);

    return ApiResponse.success("Channel marked as read", null);
    }


    @GetMapping("/api/messages/channels/{channelId}/unread-state")
    public ApiResponse<ChannelUnreadStateResponse> getUnreadState(
        @PathVariable("channelId") Long channelId,
        HttpServletRequest request) {
    UUID userId = getUserId(request);

    return ApiResponse.success(messageService.getUnreadState(channelId, userId));
}

}
