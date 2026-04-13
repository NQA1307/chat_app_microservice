package com.discordclone.serverservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.serverservice.dto.*;
import com.discordclone.serverservice.service.ServerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    /**
     * Đọc userId từ header X-User-Id được API Gateway forward sau khi validate JWT.
     */
    private Long getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            throw new RuntimeException("Missing X-User-Id header");
        }
        return Long.parseLong(userId);
    }

    // POST /api/servers — Tạo server mới
    @PostMapping
    public ResponseEntity<ApiResponse<ServerResponse>> createServer(
            @Valid @RequestBody CreateServerRequest req,
            HttpServletRequest request) {
        ServerResponse server = serverService.createServer(req, getUserId(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Server created", server));
    }

    // GET /api/servers — Lấy danh sách server mà user tham gia
    @GetMapping
    public ResponseEntity<ApiResponse<List<ServerResponse>>> getMyServers(HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(serverService.getServersForUser(getUserId(request))));
    }

    // GET /api/servers/{id} — Chi tiết 1 server
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServerResponse>> getServer(
            @PathVariable Long id,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(serverService.getServerById(id, getUserId(request))));
    }

    // POST /api/servers/{id}/channels — Tạo channel (chỉ owner)
    @PostMapping("/{id}/channels")
    public ResponseEntity<ApiResponse<ChannelResponse>> createChannel(
            @PathVariable Long id,
            @Valid @RequestBody CreateChannelRequest req,
            HttpServletRequest request) {
        ChannelResponse channel = serverService.createChannel(id, req, getUserId(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Channel created", channel));
    }

    // POST /api/servers/{id}/join — Join server
    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<Void>> joinServer(
            @PathVariable Long id,
            HttpServletRequest request) {
        serverService.joinServer(id, getUserId(request));
        return ResponseEntity.ok(ApiResponse.success("Joined server successfully", null));
    }
}
