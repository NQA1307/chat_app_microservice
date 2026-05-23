package com.discordclone.serverservice.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.discordclone.common.enums.InviteStatus;
import com.discordclone.common.enums.InviteType;
import com.discordclone.common.exception.AppException;
import com.discordclone.serverservice.dto.ChannelResponse;
import com.discordclone.serverservice.dto.InviteResponse;
import com.discordclone.serverservice.dto.SendServerInviteRequest;
import com.discordclone.serverservice.repository.InviteRepository;
import com.discordclone.serverservice.repository.ServerMemberRepository;
import com.discordclone.serverservice.repository.ServerRepository;
import com.discordclone.serverservice.entity.Invite;
import com.discordclone.serverservice.entity.Server;
import com.discordclone.serverservice.entity.ServerMember;
import com.discordclone.serverservice.dto.ServerResponse;


import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InviteService {
    

    private static final String TARGET_SERVER = "SERVER";

    private final InviteRepository inviteRepository;
    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;

    @Transactional
    public InviteResponse sendServerInvite (
        Long serverId,
        UUID senderUserId,
        SendServerInviteRequest request
    ) {

        //Kiem tra su ton tai cua server
        Server server = serverRepository.findById(serverId).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,"Server not found"));
        
        //Kiem tra nguoi gui loi moi co thuoc server khong
        if (!serverMemberRepository.existsByServerIdAndUserId(serverId, senderUserId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You are not member of this server");
        }

        //Kiem tra xem sender co tu moi ban than khong
        if (senderUserId.equals(request.receiverUserId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "You can not invite your self");
        }

        if (serverMemberRepository.existsByServerIdAndUserId(serverId, request.receiverUserId())) {
            throw new AppException(HttpStatus.CONFLICT, "User is already a member of this server");
        }

        String targetId = server.getId().toString();

        boolean duplicated = inviteRepository.existsBySenderIdAndReceiverIdAndTypeAndTargetTypeAndTargetIdAndStatus(senderUserId, 
            request.receiverUserId(), 
            InviteType.SERVER_INVITE,
            TARGET_SERVER,
            targetId,
            InviteStatus.PENDING
        );

        if (duplicated) {
            throw new AppException(HttpStatus.CONFLICT, "Server invite already sent");
        }

        Invite invite = Invite.builder()
        .senderId(senderUserId)
        .receiverId(request.receiverUserId())
        .type(InviteType.SERVER_INVITE)
        .status(InviteStatus.PENDING)
        .targetType(TARGET_SERVER)
        .targetId(targetId)
        .message(request.message())
        .expiresAt(LocalDateTime.now().plusDays(7))
        .build();

        return toInviteResponse(inviteRepository.save(invite));
    
    }

    @Transactional
    public ServerResponse acceptInvite(String inviteId, UUID receiverUserId) {
        Invite invite = inviteRepository.findByIdAndReceiverId(inviteId, receiverUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Invite not found"));

        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new AppException(HttpStatus.CONFLICT, "Invite is not pending");
        }

        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus(InviteStatus.EXPIRED);
            inviteRepository.save(invite);
            throw new AppException(HttpStatus.GONE, "Invite has expired");
        }

        if (invite.getType() != InviteType.SERVER_INVITE || !TARGET_SERVER.equals(invite.getTargetType())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid invite type");
        }

        Long serverId;
        try {
            serverId = Long.parseLong(invite.getTargetId());
        } catch (NumberFormatException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid server invite target");
        }

        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Server not found"));

        if (!serverMemberRepository.existsByServerIdAndUserId(serverId, receiverUserId)) {
            serverMemberRepository.save(ServerMember.builder()
                    .server(server)
                    .userId(receiverUserId)
                    .role(ServerMember.MemberRole.GUEST)
                    .build());
        }

        invite.setStatus(InviteStatus.ACCEPTED);
        inviteRepository.save(invite);

        return toServerResponse(server);
    }


    //Nhận lời mời trả về list inviteresponse
    public List<InviteResponse> getIncomingInvites(UUID receiverUserId) {
        return inviteRepository
        .findByReceiverIdAndStatusOrderByCreatedAtDesc(
            receiverUserId,
            InviteStatus.PENDING)
            .stream()
            .map(this::toInviteResponse)
            .toList();
    }

    @Transactional
    public void declineInvite(String inviteId, UUID receiverUserId) {
        Invite invite = inviteRepository.findByIdAndReceiverId(inviteId, receiverUserId)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Invite not found"));

        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new AppException(HttpStatus.CONFLICT,"Invite is not pending");
        }

        invite.setStatus(InviteStatus.DECLINED);
        inviteRepository.save(invite);
    }

    private InviteResponse toInviteResponse(Invite invite) {
        return new InviteResponse(
                invite.getId(),
                invite.getSenderId(),
                invite.getReceiverId(),
                invite.getType(),
                invite.getStatus(),
                invite.getTargetType(),
                invite.getTargetId(),
                resolveTargetName(invite),
                invite.getMessage(),
                invite.getExpiresAt(),
                invite.getCreatedAt()
        );
    }

    private String resolveTargetName(Invite invite) {
        if (!TARGET_SERVER.equals(invite.getTargetType())) {
            return null;
        }

        try {
            Long serverId = Long.parseLong(invite.getTargetId());
            return serverRepository.findById(serverId)
                    .map(Server::getName)
                    .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ServerResponse toServerResponse(Server server) {
        return ServerResponse.builder()
                .id(server.getId())
                .name(server.getName())
                .description(server.getDescription())
                .imageUrl(server.getImageUrl())
                .ownerId(server.getOwnerId())
                .memberCount(server.getMembers().size())
                .channels(server.getChannels().stream()
                        .map(channel -> ChannelResponse.builder()
                                .id(channel.getId())
                                .name(channel.getName())
                                .type(channel.getType().name())
                                .serverId(server.getId())
                                .build())
                        .toList())
                .createdAt(server.getCreatedAt())
                .build();
    }
}

