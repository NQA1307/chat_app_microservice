package com.discordclone.userservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.common.exception.AppException;
import com.discordclone.userservice.dto.request.EmailCodeRequest;
import com.discordclone.userservice.dto.request.LoginRequest;
import com.discordclone.userservice.dto.request.LogoutRequest;
import com.discordclone.userservice.dto.request.PasswordResetConfirmRequest;
import com.discordclone.userservice.dto.request.RegisterRequest;
import com.discordclone.userservice.dto.request.VerifyCodeRequest;
import com.discordclone.userservice.dto.response.AuthResponse;
import com.discordclone.userservice.repository.UserRepository;
import com.discordclone.userservice.security.JwtUtil;
import com.discordclone.userservice.service.AuthEmailEventPublisher;
import com.discordclone.userservice.service.AccessTokenRevocationService;
import com.discordclone.userservice.service.OtpService;
import com.discordclone.userservice.service.PendingRegistrationService;
import com.discordclone.userservice.service.PendingRegistrationService.PendingRegistration;
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
    private final AccessTokenRevocationService accessTokenRevocationService;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthEmailEventPublisher authEmailEventPublisher;
    private final OtpService otpService;
    private final PendingRegistrationService pendingRegistrationService;

    private static final String EMAIL_VERIFY = "email_verify";
    private static final String PASSWORD_RESET = "password_reset";

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request) {
        PendingRegistration pendingRegistration = userService.buildPendingRegistration(request);
        pendingRegistrationService.save(pendingRegistration);

        String code = otpService.createCode(pendingRegistration.email(), EMAIL_VERIFY);
        authEmailEventPublisher.publishEmailVerification(pendingRegistration.email(), code);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Verification code sent", null));
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
        String refreshTokenId = jwtUtil.extractTokenId(refreshToken);
        // 3. Kiem tra trong Redis xem co khop khong
        if (refreshTokenId == null || !refreshTokenService.isValid(userId, refreshTokenId, refreshToken)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Refresh token da het han hoac bi thu hoi");
        }

        // 4. Lay thong tin user de tao access token moi
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        refreshTokenService.revoke(userId, refreshTokenId);

        String newAccessToken = jwtUtil.generateAccessToken(
                userId, user.getEmail(), user.getRole().name(), user.getUsername());

        String newRefreshToken = jwtUtil.generateRefreshToken(userId, user.getEmail());
        refreshTokenService.save(userId, jwtUtil.extractTokenId(newRefreshToken), newRefreshToken);

        return ResponseEntity.ok(ApiResponse.success(
                AuthResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .displayName(user.getDisplayName())
                        .avatarUrl(user.getAvatarUrl())
                        .emailVerified(user.isEmailVerified())
                        .build()
        ));
    }

    // Dang xuat - xoa refresh token khoi Redis
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        UUID userId = jwtUtil.extractUserId(refreshToken);
        String refreshTokenId = jwtUtil.extractTokenId(refreshToken);
        if (refreshTokenId == null || !refreshTokenService.isValid(userId, refreshTokenId, refreshToken)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }

        refreshTokenService.revoke(userId, refreshTokenId);
        accessTokenRevocationService.revokeToken(authorizationHeader);

        return ResponseEntity.ok(ApiResponse.success(null));
    }


    //Gui xacs thuc email
    @PostMapping("/email-verification/send")
    public ResponseEntity<ApiResponse<Void>> sendEmailVerificationCode(
            @Valid @RequestBody EmailCodeRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        PendingRegistration pendingRegistration = pendingRegistrationService.get(email);
        if (pendingRegistration != null) {
            userService.assertRegistrationAvailable(pendingRegistration.email(), pendingRegistration.username());

            String code = otpService.createCode(email, EMAIL_VERIFY);
            authEmailEventPublisher.publishEmailVerification(email, code);

            return ResponseEntity.ok(ApiResponse.success("Verification code sent", null));
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Registration session not found"));

        if (user.isEmailVerified()) {
            return ResponseEntity.ok(ApiResponse.success("Email already verified", null));
        }

        String code = otpService.createCode(email, EMAIL_VERIFY);
        authEmailEventPublisher.publishEmailVerification(email, code);

        return ResponseEntity.ok(ApiResponse.success("Verification code sent", null));
    }


    //Xac thuc email
    @PostMapping("/email-verification/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(
            @Valid @RequestBody VerifyCodeRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        otpService.verifyCode(email, request.getCode(), EMAIL_VERIFY);

        PendingRegistration pendingRegistration = pendingRegistrationService.get(email);
        if (pendingRegistration != null) {
            AuthResponse response = userService.completePendingRegistration(pendingRegistration);
            pendingRegistrationService.delete(email);
            return ResponseEntity.ok(ApiResponse.success("Email verified", response));
        }

        userService.markEmailVerified(email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        String accessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());
        refreshTokenService.save(user.getId(), jwtUtil.extractTokenId(refreshToken), refreshToken);

        return ResponseEntity.ok(ApiResponse.success("Email verified",
                AuthResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .displayName(user.getDisplayName())
                        .avatarUrl(user.getAvatarUrl())
                        .emailVerified(user.isEmailVerified())
                        .build()));
    }

    //Gui yeu cau reset mat khau
    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody EmailCodeRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        userRepository.findByEmail(email).ifPresent(user -> {
            String code = otpService.createCode(email, PASSWORD_RESET);
            authEmailEventPublisher.publishPasswordReset(email, code);
        });

        return ResponseEntity.ok(ApiResponse.success(
                "If the email exists, a reset code has been sent",
                null
        ));
    }


    //Xac thuc yeu cau reset mat khau
    @PostMapping("/password-reset/verify")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyPasswordResetCode(
            @Valid @RequestBody VerifyCodeRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Code expired or invalid"));

        otpService.verifyCode(email, request.getCode(), PASSWORD_RESET);
        String resetToken = otpService.createPasswordResetSession(user.getId());

        return ResponseEntity.ok(ApiResponse.success(Map.of("resetToken", resetToken)));
    }

    //Confirm reset mat khau
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {

        UUID userId = otpService.consumePasswordResetSession(request.getResetToken());
        userService.resetPassword(userId, request.getNewPassword());

        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }
}
