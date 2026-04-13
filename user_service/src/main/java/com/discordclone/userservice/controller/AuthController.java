package com.discordclone.userservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.userservice.dto.LoginRequest;
import com.discordclone.userservice.dto.RegisterRequest;
import com.discordclone.userservice.dto.UserResponse;
import com.discordclone.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        UserResponse user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", user));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(
            @Valid @RequestBody LoginRequest request) {
        String token = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("token", token)));
    }
}
