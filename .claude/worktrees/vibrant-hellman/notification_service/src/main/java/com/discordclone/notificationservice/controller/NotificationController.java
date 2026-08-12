package com.discordclone.notificationservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.common.exception.AppException;
import com.discordclone.notificationservice.dto.NotificationResponse;
import com.discordclone.notificationservice.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private UUID getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }
        return UUID.fromString(userId);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotifications(getUserId(request))));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(HttpServletRequest request) {
        long count = notificationService.countUnread(getUserId(request));
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable("id") String id,
            HttpServletRequest request
    ) {
        NotificationResponse response = notificationService.markAsRead(id, getUserId(request));
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", response));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(HttpServletRequest request) {
        notificationService.markAllAsRead(getUserId(request));
        return ResponseEntity.ok(ApiResponse.success("Notifications marked as read", null));
    }

    //Danh dau tin nhan la da doc
    @PatchMapping("/source/{sourceType}/{sourceId}/read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markSourceAsRead(
            @PathVariable("sourceType") String sourceType,
            @PathVariable("sourceId") String sourceId,
            HttpServletRequest request
    ) {
        int updatedCount = notificationService.markSourceAsRead(getUserId(request), sourceType, sourceId);
        return ResponseEntity.ok(ApiResponse.success(
                "Notifications marked as read",
                Map.of("updatedCount", updatedCount)
        ));
    }
}
