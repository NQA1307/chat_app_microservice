package com.discordclone.userservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.userservice.dto.request.BatchDevicePublicKeysRequest;
import com.discordclone.userservice.dto.request.RegisterDeviceKeyRequest;
import com.discordclone.userservice.dto.request.RegisterPushTokenRequest;
import com.discordclone.userservice.dto.request.UpdateProfileRequest;
import com.discordclone.userservice.dto.response.DevicePublicKeyResponse;
import com.discordclone.userservice.dto.response.UserResponse;
import com.discordclone.userservice.entity.UserDeviceKey;
import com.discordclone.userservice.repository.UserDeviceKeyRepository;
import com.discordclone.userservice.service.CloudinaryService;
import com.discordclone.userservice.service.MeiliSearchService;
import com.discordclone.userservice.service.UserService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    private final UserService userService;
    private final MeiliSearchService meiliSearchService;
    private final CloudinaryService cloudinaryService;
    private final UserDeviceKeyRepository userDeviceKeyRepository;

    public UserController(UserService userService,
            MeiliSearchService meiliSearchService,
            CloudinaryService cloudinaryService,
            UserDeviceKeyRepository userDeviceKeyRepository) {
        this.userService = userService;
        this.meiliSearchService = meiliSearchService;
        this.cloudinaryService = cloudinaryService;
        this.userDeviceKeyRepository = userDeviceKeyRepository;
    }

    /**
     * GET /api/users/me
     * API Gateway forwards the validated user's email in X-User-Email header.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByEmail(email)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully",
                userService.updateProfile(email, request)));
    }

    @GetMapping("/batch")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByIds(
            @RequestParam("ids") List<UUID> ids) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUsersByIds(ids)));
    }

    @PostMapping("/me/devices")
    public ResponseEntity<ApiResponse<DevicePublicKeyResponse>> registerDeviceKey(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody RegisterDeviceKeyRequest request) {
        UserDeviceKey deviceKey = userDeviceKeyRepository.findByUserIdAndDeviceId(userId, request.deviceId())
                .orElseGet(() -> UserDeviceKey.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .deviceId(request.deviceId())
                        .build());

        deviceKey.setPublicKey(request.publicKey());
        deviceKey.setAlgorithm(request.algorithm() == null || request.algorithm().isBlank()
                ? "ECDH-P256"
                : request.algorithm());
        deviceKey.setRevokedAt(null);

        UserDeviceKey saved = userDeviceKeyRepository.save(deviceKey);
        return ResponseEntity.ok(ApiResponse.success("Device key registered", toDevicePublicKeyResponse(saved)));
    }

    @PostMapping("/me/push-token")
    public ResponseEntity<ApiResponse<Void>> registerPushToken(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody RegisterPushTokenRequest request) {
        UserDeviceKey deviceKey = userDeviceKeyRepository.findByUserIdAndDeviceId(userId, request.deviceId())
                .orElseGet(() -> UserDeviceKey.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .deviceId(request.deviceId())
                        .build());

        userDeviceKeyRepository.findByPushTokenAndRevokedAtIsNull(request.pushToken())
                .stream()
                .filter(existing -> !existing.getUserId().equals(userId)
                        || !existing.getDeviceId().equals(request.deviceId()))
                .forEach(existing -> {
                    existing.setPushToken(null);
                    existing.setRevokedAt(LocalDateTime.now());
                    userDeviceKeyRepository.save(existing);
                });

        deviceKey.setPushToken(request.pushToken());
        deviceKey.setRevokedAt(null);
        userDeviceKeyRepository.save(deviceKey);
        return ResponseEntity.ok(ApiResponse.success("Push token registered", null));
    }

    @PostMapping("/devices/public-keys/batch")
    public ResponseEntity<ApiResponse<List<DevicePublicKeyResponse>>> getDevicePublicKeys(
            @Valid @RequestBody BatchDevicePublicKeysRequest request) {
        List<DevicePublicKeyResponse> keys = userDeviceKeyRepository
                .findByUserIdInAndRevokedAtIsNull(request.userIds())
                .stream()
                .map(this::toDevicePublicKeyResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(keys));
    }

    @DeleteMapping("/me/devices/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> revokeDeviceKey(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("deviceId") String deviceId) {
        userDeviceKeyRepository.findByUserIdAndDeviceId(userId, deviceId)
                .ifPresent(deviceKey -> {
                    deviceKey.setRevokedAt(LocalDateTime.now());
                    userDeviceKeyRepository.save(deviceKey);
                });

        return ResponseEntity.ok(ApiResponse.success("Device key revoked", null));
    }

    /**
     * GET /api/users/{id}
     * Used internally by other services (server-service, message-service).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PostMapping("/sync-meili")
    public ResponseEntity<ApiResponse<Integer>> syncMeili() {
        int count = meiliSearchService.syncAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Successfully synced users to Meilisearch", count));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam("q") String query,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader Map<String, String> allHeaders) {

        // Log headers for debugging
        if (userId == null) {
            log.warn("X-User-Id is MISSING. All headers: {}", allHeaders.keySet());
        } else {
            log.info("Search request from userId={}, query='{}'", userId, query);
        }

        UUID currentUserId = null;
        if (userId != null) {
            try {
                currentUserId = UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID format for X-User-Id: {}", userId);
            }
        }

        return ResponseEntity.ok(ApiResponse.success(userService.searchUsers(query, currentUserId)));
    }

    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<UserResponse>> updateAvatar(
            @RequestHeader("X-User-Email") String email,
            @RequestPart("file") MultipartFile file) {
        log.info("Avatar upload request from: {}, file size: {} bytes", email, file.getSize());
        String imageUrl = cloudinaryService.uploadFile(file);
        return ResponseEntity
                .ok(ApiResponse.success("Avatar updated successfully", userService.updateAvatar(email, imageUrl)));
    }

    private DevicePublicKeyResponse toDevicePublicKeyResponse(UserDeviceKey deviceKey) {
        return new DevicePublicKeyResponse(
                deviceKey.getUserId(),
                deviceKey.getDeviceId(),
                deviceKey.getPublicKey(),
                deviceKey.getAlgorithm());
    }
}
