package com.discordclone.userservice.service;

import com.discordclone.common.event.EventTypes;
import com.discordclone.common.event.FriendRequestEvent;
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
    private final DomainEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Value("${app.rabbitmq.friend-request-sent-routing-key}")
    private String friendRequestSentRoutingKey;

    @org.springframework.beans.factory.annotation.Value("${app.rabbitmq.friend-request-accepted-routing-key}")
    private String friendRequestAcceptedRoutingKey;

    @Transactional
    public FriendResponse sendFriendRequest(UUID currentUserId, SendFriendRequest request) {
        UUID targetUserId = request.getTargetUserId();

        if (currentUserId.equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot send a friend request to yourself");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Current user not found"));

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
            Friendship saved = friendshipRepository.save(existing);
            
            publishFriendRequestEvent(currentUser, targetUserId, "SENT");
            
            return toResponse(saved, targetUser, currentUserId);
        }

        Friendship friendship = Friendship.builder()
                .requesterId(currentUserId)
                .receiverId(targetUserId)
                .status(FriendshipStatus.PENDING)
                .build();

        Friendship saved = friendshipRepository.save(friendship);
        
        publishFriendRequestEvent(currentUser, targetUserId, "SENT");

        return toResponse(saved, targetUser, currentUserId);
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
        Friendship saved = friendshipRepository.save(friendship);
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        publishFriendRequestEvent(currentUser, friendship.getRequesterId(), "ACCEPTED");

        return toResponse(saved, loadUser(friendship.getRequesterId()), currentUserId);
    }

    private void publishFriendRequestEvent(User sender, UUID receiverId, String type) {
        String eventType = "SENT".equals(type) ? EventTypes.FRIEND_REQUEST_SENT : EventTypes.FRIEND_REQUEST_ACCEPTED;
        String routingKey = "SENT".equals(type) ? friendRequestSentRoutingKey : friendRequestAcceptedRoutingKey;

        FriendRequestEvent event = new FriendRequestEvent(
                sender.getId(),
                sender.getUsername(),
                receiverId,
                type
        );

        eventPublisher.publish(eventType, routingKey, event);
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

    @Transactional
    public FriendResponse blockUser(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot block yourself");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Target user not found"));

        Friendship friendship = friendshipRepository.findBetweenUsers(currentUserId, targetUserId).orElse(null);
        if (friendship == null) {
            friendship = Friendship.builder()
                    .requesterId(currentUserId)
                    .receiverId(targetUserId)
                    .status(FriendshipStatus.BLOCKED)
                    .actionUserId(currentUserId)
                    .build();
        } else {
            friendship.setStatus(FriendshipStatus.BLOCKED);
            friendship.setActionUserId(currentUserId);
            // reset requester and receiver to align with who did the block
            friendship.setRequesterId(currentUserId);
            friendship.setReceiverId(targetUserId);
        }

        Friendship saved = friendshipRepository.save(friendship);
        
        eventPublisher.publish(
                com.discordclone.common.event.EventTypes.USER_BLOCKED,
                "user.blocked",
                new com.discordclone.common.event.UserBlockEvent(currentUserId, targetUserId, true)
        );

        return toResponse(saved, targetUser, currentUserId);
    }

    @Transactional
    public void unblockUser(UUID currentUserId, UUID targetUserId) {
        Friendship friendship = friendshipRepository.findBetweenUsers(currentUserId, targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Blocked relationship not found"));

        if (friendship.getStatus() != FriendshipStatus.BLOCKED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Relationship is not blocked");
        }

        if (friendship.getActionUserId() != null && !friendship.getActionUserId().equals(currentUserId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only the user who initiated the block can unblock");
        }

        friendshipRepository.delete(friendship);

        eventPublisher.publish(
                com.discordclone.common.event.EventTypes.USER_BLOCKED,
                "user.blocked",
                new com.discordclone.common.event.UserBlockEvent(currentUserId, targetUserId, false)
        );
    }

    public List<FriendResponse> getBlockedFriends(UUID currentUserId) {
        return friendshipRepository.findByUserIdAndStatus(currentUserId, FriendshipStatus.BLOCKED)
                .stream()
                .filter(f -> f.getActionUserId() == null || f.getActionUserId().equals(currentUserId))
                .map(friendship -> {
                    UUID friendUserId = friendship.getRequesterId().equals(currentUserId)
                            ? friendship.getReceiverId()
                            : friendship.getRequesterId();
                    return toResponse(friendship, loadUser(friendUserId), currentUserId);
                })
                .toList();
    }

    public com.discordclone.userservice.dto.response.RelationshipResponse getRelationship(UUID currentUserId, UUID targetUserId) {
        Friendship friendship = friendshipRepository.findBetweenUsers(currentUserId, targetUserId).orElse(null);
        
        if (friendship == null || friendship.getStatus() == FriendshipStatus.DECLINED) {
            return com.discordclone.userservice.dto.response.RelationshipResponse.builder()
                    .targetUserId(targetUserId)
                    .status("NONE")
                    .build();
        }

        String statusStr = "NONE";
        switch (friendship.getStatus()) {
            case ACCEPTED:
                statusStr = "FRIEND";
                break;
            case PENDING:
                statusStr = friendship.getRequesterId().equals(currentUserId) ? "PENDING_OUTGOING" : "PENDING_INCOMING";
                break;
            case BLOCKED:
                if (friendship.getActionUserId() == null) {
                    statusStr = friendship.getRequesterId().equals(currentUserId) ? "BLOCKED_BY_ME" : "BLOCKED_ME";
                } else {
                    statusStr = friendship.getActionUserId().equals(currentUserId) ? "BLOCKED_BY_ME" : "BLOCKED_ME";
                }
                break;
            default:
                statusStr = "NONE";
        }

        return com.discordclone.userservice.dto.response.RelationshipResponse.builder()
                .targetUserId(targetUserId)
                .status(statusStr)
                .build();
    }
}
