package com.discordclone.userservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.common.exception.AppException;
import com.discordclone.userservice.dto.request.LoginRequest;
import com.discordclone.userservice.dto.request.RegisterRequest;
import com.discordclone.userservice.dto.response.AuthResponse;
import com.discordclone.userservice.repository.UserRepository;
import com.discordclone.userservice.security.JwtUtil;
import com.discordclone.userservice.service.RefreshTokenService;
import com.discordclone.userservice.service.UserService;
import com.discordclone.userservice.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Cap access token moi tu refresh token
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody Map<String, String> body) {

        String refreshToken = body.get("refreshToken");

        // 1. Kiem tra token co hop le va dung loai khong
        if (refreshToken == null
                || !jwtUtil.validateToken(refreshToken)
                || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Refresh token khong hop le");
        }

        // 2. Extract thong tin tu token
        UUID userId = jwtUtil.extractUserId(refreshToken);
        String email = jwtUtil.extractEmail(refreshToken);

        // 3. Kiem tra trong Redis xem co khop khong
        if (!refreshTokenService.isValid(userId, refreshToken)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Refresh token da het han hoac bi thu hoi");
        }

        // 4. Lay thong tin user de tao access token moi
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        String newAccessToken = jwtUtil.generateAccessToken(
                userId, email, user.getRole().name(), user.getUsername());

        return ResponseEntity.ok(ApiResponse.success(
                AuthResponse.builder().accessToken(newAccessToken).build()
        ));
    }

    // Dang xuat - xoa refresh token khoi Redis
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("X-User-Id") String userId) {
        refreshTokenService.revoke(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
