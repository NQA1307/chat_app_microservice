package com.discordclone.userservice.service;

import com.discordclone.common.exception.AppException;
import com.discordclone.userservice.dto.request.SendFriendRequest;
import com.discordclone.userservice.dto.response.FriendResponse;
import com.discordclone.userservice.entity.Friendship;
import com.discordclone.userservice.entity.Friendship.FriendshipStatus;
import com.discordclone.userservice.entity.User;
import com.discordclone.userservice.repository.FriendshipRepository;
import com.discordclone.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Transactional
    public FriendResponse sendFriendRequest(UUID currentUserId, SendFriendRequest request) {
        UUID targetUserId = request.getTargetUserId();

        if (currentUserId.equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot send a friend request to yourself");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Target user not found"));

        Friendship existing = friendshipRepository.findBetweenUsers(currentUserId, targetUserId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new AppException(HttpStatus.CONFLICT, "Users are already friends");
            }
            if (existing.getStatus() == FriendshipStatus.PENDING) {
                throw new AppException(HttpStatus.CONFLICT, "Friend request already exists");
            }
            if (existing.getStatus() == FriendshipStatus.BLOCKED) {
                throw new AppException(HttpStatus.CONFLICT, "Cannot send friend request");
            }

            existing.setRequesterId(currentUserId);
            existing.setReceiverId(targetUserId);
            existing.setStatus(FriendshipStatus.PENDING);
            return toResponse(friendshipRepository.save(existing), targetUser, currentUserId);
        }

        Friendship friendship = Friendship.builder()
                .requesterId(currentUserId)
                .receiverId(targetUserId)
                .status(FriendshipStatus.PENDING)
                .build();

        return toResponse(friendshipRepository.save(friendship), targetUser, currentUserId);
    }

    public List<FriendResponse> getIncomingRequests(UUID currentUserId) {
        return friendshipRepository.findByReceiverIdAndStatus(currentUserId, FriendshipStatus.PENDING)
                .stream()
                .map(friendship -> toResponse(friendship, loadUser(friendship.getRequesterId()), currentUserId))
                .toList();
    }

    public List<FriendResponse> getOutgoingRequests(UUID currentUserId) {
        return friendshipRepository.findByRequesterIdAndStatus(currentUserId, FriendshipStatus.PENDING)
                .stream()
                .map(friendship -> toResponse(friendship, loadUser(friendship.getReceiverId()), currentUserId))
                .toList();
    }

    public List<FriendResponse> getFriends(UUID currentUserId) {
        return friendshipRepository.findByUserIdAndStatus(currentUserId, FriendshipStatus.ACCEPTED)
                .stream()
                .map(friendship -> {
                    UUID friendUserId = friendship.getRequesterId().equals(currentUserId)
                            ? friendship.getReceiverId()
                            : friendship.getRequesterId();
                    return toResponse(friendship, loadUser(friendUserId), currentUserId);
                })
                .toList();
    }

    @Transactional
    public FriendResponse acceptRequest(UUID currentUserId, UUID friendshipId) {
        Friendship friendship = loadFriendship(friendshipId);
        if (!friendship.getReceiverId().equals(currentUserId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only the request receiver can accept this request");
        }
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new AppException(HttpStatus.CONFLICT, "Friend request is not pending");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        return toResponse(friendshipRepository.save(friendship), loadUser(friendship.getRequesterId()), currentUserId);
    }

    @Transactional
    public void declineRequest(UUID currentUserId, UUID friendshipId) {
        Friendship friendship = loadFriendship(friendshipId);
        if (!friendship.getReceiverId().equals(currentUserId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only the request receiver can decline this request");
        }
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new AppException(HttpStatus.CONFLICT, "Friend request is not pending");
        }

        friendship.setStatus(FriendshipStatus.DECLINED);
        friendshipRepository.save(friendship);
    }

    @Transactional
    public void cancelOutgoingRequest(UUID currentUserId, UUID friendshipId) {
        Friendship friendship = loadFriendship(friendshipId);
        if (!friendship.getRequesterId().equals(currentUserId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only the requester can cancel this request");
        }
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new AppException(HttpStatus.CONFLICT, "Friend request is not pending");
        }

        friendshipRepository.delete(friendship);
    }

    @Transactional
    public void removeFriend(UUID currentUserId, UUID friendUserId) {
        Friendship friendship = friendshipRepository.findBetweenUsers(currentUserId, friendUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Friendship not found"));

        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new AppException(HttpStatus.CONFLICT, "Users are not friends");
        }

        friendshipRepository.delete(friendship);
    }

    private Friendship loadFriendship(UUID friendshipId) {
        return friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Friend request not found"));
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private FriendResponse toResponse(Friendship friendship, User user, UUID currentUserId) {
        String direction = friendship.getRequesterId().equals(currentUserId) ? "OUTGOING" : "INCOMING";

        return FriendResponse.builder()
                .friendshipId(friendship.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .status(friendship.getStatus().name())
                .direction(direction)
                .createdAt(friendship.getCreatedAt())
                .updatedAt(friendship.getUpdatedAt())
                .build();
    }
}
