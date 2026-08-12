package com.discordclone.userservice.service;

import com.discordclone.common.exception.AppException;
import com.discordclone.userservice.dto.request.LoginRequest;
import com.discordclone.userservice.dto.request.RegisterRequest;
import com.discordclone.userservice.dto.request.UpdateProfileRequest;
import com.discordclone.userservice.dto.response.AuthResponse;
import com.discordclone.userservice.dto.response.UserResponse;
import com.discordclone.userservice.entity.User;
import com.discordclone.userservice.repository.UserRepository;
import com.discordclone.userservice.security.JwtUtil;
import com.discordclone.userservice.service.PendingRegistrationService.PendingRegistration;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class UserService {

    private final RefreshTokenService refreshTokenService;
    private final AccessTokenRevocationService accessTokenRevocationService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MeiliSearchService meiliSearchService;

    // ── Register ───────────────────────────────────────────────
    @Transactional
    public com.discordclone.userservice.dto.response.AuthResponse register(RegisterRequest request) {
        PendingRegistration pending = buildPendingRegistration(request);
        return completePendingRegistration(pending);
    }

    public PendingRegistration buildPendingRegistration(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String username = request.getUsername().trim();
        
        assertRegistrationAvailable(email, username);

        return new PendingRegistration(
                email,
                username,
                request.getDisplayName() != null ? request.getDisplayName().trim() : username,
                passwordEncoder.encode(request.getPassword())
        );
    }

    public void assertRegistrationAvailable(String email, String username) {
        if (userRepository.existsByEmail(email)) {
            throw new AppException(HttpStatus.CONFLICT, "Email already in use");
        }
        if (userRepository.existsByUsername(username)) {
            throw new AppException(HttpStatus.CONFLICT, "Username already taken");
        }
    }

    @Transactional
    public AuthResponse completePendingRegistration(PendingRegistration pending) {
        User user = User.builder()
                .username(pending.username())
                .email(pending.email())
                .password(pending.passwordHash())
                .displayName(pending.displayName())
                .role(User.Role.USER)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);
        meiliSearchService.indexNewUser(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name(),
                user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());
        refreshTokenService.save(user.getId(), jwtUtil.extractTokenId(refreshToken), refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(true)
                .build();
    }

    @Transactional
    public void markEmailVerified(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        refreshTokenService.revokeAll(userId);
        accessTokenRevocationService.revokeAllForUser(userId);
    }

    // ── Login ──────────────────────────────────────────────────
    public com.discordclone.userservice.dto.response.AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (user.isBanned()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Account is banned");
        }

        if (user.isLocked()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Account is locked");
        }

        if (!user.isEmailVerified()) {
            return AuthResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .displayName(user.getDisplayName())
                    .avatarUrl(user.getAvatarUrl())
                    .emailVerified(false)
                    .build();
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());
        refreshTokenService.save(user.getId(), jwtUtil.extractTokenId(refreshToken), refreshToken);

        log.info("User {} logged in with avatar: {}", user.getEmail(), user.getAvatarUrl());

        return com.discordclone.userservice.dto.response.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(true)
                .build();
    }

    // ── Get User ───────────────────────────────────────────────
    public UserResponse getUserById(java.util.UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(user);
    }

    // ── Mapper ─────────────────────────────────────────────────
    public List<UserResponse> getUsersByIds(List<java.util.UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return userRepository.findByIdIn(ids)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .bannerColor(user.getBannerColor())
                .customStatus(user.getCustomStatus())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .emailVerified(user.isEmailVerified())
                .build();
    }

    public List<UserResponse> searchUsers(String query, java.util.UUID currentUserId) {
        List<Map<String, Object>> hits = meiliSearchService.searchUsers(query, 10);

        return hits.stream()
                .filter(hit -> {
                    try {
                        Object idObject = hit.get("id");
                        if (idObject == null) return false;
                        java.util.UUID hitId = java.util.UUID.fromString(idObject.toString());
                        return !hitId.equals(currentUserId);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(hit -> {
                    try {
                        Object mapIdObj = hit.get("id");
                        if (mapIdObj == null) return null;
                        
                        java.util.UUID mapId = java.util.UUID.fromString(mapIdObj.toString());
                        String username = (String) hit.get("username");
                        String displayName = (String) hit.get("displayName");
                        String avatarUrl = (String) hit.get("avatar");
                        
                        log.debug("Mapping hit to UserResponse: id={}, username={}, displayName={}", mapId, username, displayName);
                        
                        return UserResponse.builder()
                                .id(mapId)
                                .username(username)
                                .displayName(displayName)
                                .avatarUrl(avatarUrl)
                                .build();
                    } catch (Exception e) {
                        log.error("Error mapping Meilisearch hit: {}. Hit data: {}", e.getMessage(), hit);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public UserResponse updateAvatar(String email, String imageUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        user.setAvatarUrl(imageUrl);
        user = userRepository.save(user);
        
        meiliSearchService.indexNewUser(user);
        
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getDisplayName() != null) {
            String displayName = request.getDisplayName().trim();
            user.setDisplayName(displayName.isBlank() ? user.getUsername() : displayName);
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        if (request.getBannerColor() != null) {
            user.setBannerColor(request.getBannerColor());
        }

        if (request.getCustomStatus() != null) {
            user.setCustomStatus(request.getCustomStatus());
        }

        user = userRepository.save(user);
        meiliSearchService.indexNewUser(user);

        return toResponse(user);
    }
}
