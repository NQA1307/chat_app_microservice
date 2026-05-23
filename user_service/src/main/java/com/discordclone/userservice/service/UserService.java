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
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class UserService {

    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MeiliSearchService meiliSearchService;

    

    // ── Register ───────────────────────────────────────────────
    @Transactional
    public com.discordclone.userservice.dto.response.AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(HttpStatus.CONFLICT, "Email already in use");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(HttpStatus.CONFLICT, "Username already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .role(User.Role.USER)
                .build();

        user = userRepository.save(user);
        meiliSearchService.indexNewUser(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name(),
                user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());
        refreshTokenService.save(user.getId(), refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    // ── Login ──────────────────────────────────────────────────
    public com.discordclone.userservice.dto.response.AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());
        refreshTokenService.save(user.getId(), refreshToken);

        log.info("User {} logged in with avatar: {}", user.getEmail(), user.getAvatarUrl());

        return com.discordclone.userservice.dto.response.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
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
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
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
                        return false; // Bo qua neu ID khong hop le UUID
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
        
        // Cập nhật lại Meilisearch để tìm kiếm thấy ảnh mới
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

        user = userRepository.save(user);
        meiliSearchService.indexNewUser(user);

        return toResponse(user);
    }
}
