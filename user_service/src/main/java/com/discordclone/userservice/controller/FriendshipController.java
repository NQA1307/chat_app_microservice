package com.discordclone.userservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.userservice.dto.request.SendFriendRequest;
import com.discordclone.userservice.dto.response.FriendResponse;
import com.discordclone.userservice.service.FriendshipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<FriendResponse>> sendFriendRequest(
            @Valid @RequestBody SendFriendRequest request,
            HttpServletRequest servletRequest) {
        UUID currentUserId = getCurrentUserId(servletRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Friend request sent",
                friendshipService.sendFriendRequest(currentUserId, request)
        ));
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<ApiResponse<List<FriendResponse>>> getIncomingRequests(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(friendshipService.getIncomingRequests(getCurrentUserId(request))));
    }

    @GetMapping("/requests/outgoing")
    public ResponseEntity<ApiResponse<List<FriendResponse>>> getOutgoingRequests(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(friendshipService.getOutgoingRequests(getCurrentUserId(request))));
    }

    @PostMapping("/requests/{friendshipId}/accept")
    public ResponseEntity<ApiResponse<FriendResponse>> acceptRequest(
            @PathVariable("friendshipId") UUID friendshipId,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Friend request accepted",
                friendshipService.acceptRequest(getCurrentUserId(request), friendshipId)
        ));
    }

    @PostMapping("/requests/{friendshipId}/decline")
    public ResponseEntity<ApiResponse<Void>> declineRequest(
            @PathVariable("friendshipId") UUID friendshipId,
            HttpServletRequest request) {
        friendshipService.declineRequest(getCurrentUserId(request), friendshipId);
        return ResponseEntity.ok(ApiResponse.success("Friend request declined", null));
    }

    @DeleteMapping("/requests/{friendshipId}")
    public ResponseEntity<ApiResponse<Void>> cancelOutgoingRequest(
            @PathVariable("friendshipId") UUID friendshipId,
            HttpServletRequest request) {
        friendshipService.cancelOutgoingRequest(getCurrentUserId(request), friendshipId);
        return ResponseEntity.ok(ApiResponse.success("Friend request cancelled", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendResponse>>> getFriends(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(friendshipService.getFriends(getCurrentUserId(request))));
    }

    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<ApiResponse<Void>> removeFriend(
            @PathVariable("friendUserId") UUID friendUserId,
            HttpServletRequest request) {
        friendshipService.removeFriend(getCurrentUserId(request), friendUserId);
        return ResponseEntity.ok(ApiResponse.success("Friend removed", null));
    }

    private UUID getCurrentUserId(HttpServletRequest request) {
        return UUID.fromString(request.getHeader("X-User-Id"));
    }
}
