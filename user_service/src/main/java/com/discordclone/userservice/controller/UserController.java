package com.discordclone.userservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.userservice.dto.UserResponse;
import com.discordclone.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/me
     * API Gateway forwards the validated user's email in X-User-Email header.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByEmail(email)));
    }

    /**
     * GET /api/users/{id}
     * Used internally by other services (server-service, message-service).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }
}
