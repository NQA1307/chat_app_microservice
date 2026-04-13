package com.discordclone.messageservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.messageservice.dto.MessageResponse;
import com.discordclone.messageservice.dto.SendMessageRequest;
import com.discordclone.messageservice.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * STOMP: Client gửi tới /app/chat.send
     * Server lưu DB và broadcast về /topic/channel/{channelId}
     */
    @MessageMapping("/chat.send")
    public void sendViaWebSocket(@Payload SendMessageRequest req) {
        messageService.sendMessage(req);
    }

    /**
     * REST: POST /api/messages
     * Lấy userId và username từ header do API Gateway forward
     */
    @PostMapping("/api/messages")
    public ApiResponse<MessageResponse> sendViaRest(
            @RequestBody SendMessageRequest req,
            HttpServletRequest request) {
        req.setSenderId(Long.parseLong(request.getHeader("X-User-Id")));
        req.setSenderUsername(request.getHeader("X-User-Username"));
        return ApiResponse.success(messageService.sendMessage(req));
    }

    /**
     * REST: GET /api/messages/{channelId}?page=0&size=50
     * Lấy lịch sử tin nhắn theo channel (có phân trang)
     */
    @GetMapping("/api/messages/{channelId}")
    public ApiResponse<List<MessageResponse>> getMessages(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(messageService.getMessages(channelId, page, size));
    }
}
