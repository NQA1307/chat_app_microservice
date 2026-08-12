package com.discordclone.userservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.userservice.entity.Friendship;
import com.discordclone.userservice.entity.Friendship.FriendshipStatus;
import com.discordclone.userservice.entity.UserDeviceKey;
import com.discordclone.userservice.repository.FriendshipRepository;
import com.discordclone.userservice.repository.UserDeviceKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final FriendshipRepository friendshipRepository;
    private final UserDeviceKeyRepository userDeviceKeyRepository;

    @GetMapping("/{userId}/blocks/{targetUserId}")
    public ResponseEntity<ApiResponse<BlockStatusResponse>> checkBlockStatus(
            @PathVariable("userId") UUID userId,
            @PathVariable("targetUserId") UUID targetUserId
    ) {
        Optional<Friendship> friendshipOpt = friendshipRepository.findBetweenUsers(userId, targetUserId);
        
        if (friendshipOpt.isPresent() && friendshipOpt.get().getStatus() == FriendshipStatus.BLOCKED) {
            Friendship friendship = friendshipOpt.get();
            String blockedBy = "BOTH";
            if (friendship.getActionUserId() != null) {
                blockedBy = friendship.getActionUserId().equals(userId) ? "A" : "B";
            } else {
                blockedBy = friendship.getRequesterId().equals(userId) ? "A" : "B";
            }
            return ResponseEntity.ok(ApiResponse.success(new BlockStatusResponse(true, blockedBy)));
        }

        return ResponseEntity.ok(ApiResponse.success(new BlockStatusResponse(false, null)));
    }

    @PostMapping("/{userId}/blocked-relationship/filter")
    public ResponseEntity<ApiResponse<List<UUID>>> filterBlockedPresence(
            @PathVariable("userId") UUID userId,
            @RequestBody List<UUID> targetUserIds
    ) {
        List<Friendship> blockedFriendships = friendshipRepository.findByUserIdAndStatus(userId, FriendshipStatus.BLOCKED);
        Set<UUID> blockedUserIds = blockedFriendships.stream()
                .map(f -> f.getRequesterId().equals(userId) ? f.getReceiverId() : f.getRequesterId())
                .collect(Collectors.toSet());

        List<UUID> allowedUserIds = targetUserIds.stream()
                .filter(id -> !blockedUserIds.contains(id))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(allowedUserIds));
    }

    @GetMapping("/{userId}/friends/ids")
    public ResponseEntity<ApiResponse<List<UUID>>> getFriendIds(@PathVariable("userId") UUID userId) {
        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED);
        List<UUID> friendIds = friendships.stream()
                .map(f -> f.getRequesterId().equals(userId) ? f.getReceiverId() : f.getRequesterId())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(friendIds));
    }

    @GetMapping("/{userId}/push-tokens")
    public ResponseEntity<ApiResponse<List<String>>> getPushTokens(@PathVariable("userId") UUID userId) {
        List<String> tokens = userDeviceKeyRepository.findByUserIdAndRevokedAtIsNull(userId)
                .stream()
                .map(UserDeviceKey::getPushToken)
                .filter(token -> token != null && !token.isBlank())
                .distinct()
                .toList();

        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    @PostMapping("/push-tokens/revoke")
    public ResponseEntity<ApiResponse<Void>> revokePushToken(@RequestBody RevokePushTokenRequest request) {
        userDeviceKeyRepository.findByPushTokenAndRevokedAtIsNull(request.pushToken())
                .forEach(deviceKey -> {
                    deviceKey.setPushToken(null);
                    userDeviceKeyRepository.save(deviceKey);
                });

        return ResponseEntity.ok(ApiResponse.success("Push token revoked", null));
    }

    public record BlockStatusResponse(boolean blocked, String blockedBy) {}
    public record RevokePushTokenRequest(String pushToken) {}
}
