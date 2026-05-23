package com.discordclone.serverservice.service;

import com.discordclone.common.enums.PermissionCode;
import com.discordclone.common.exception.AppException;
import com.discordclone.serverservice.dto.*;
import com.discordclone.serverservice.entity.*;
import com.discordclone.serverservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServerService {

    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    private final ServerMemberRepository memberRepository;
    private final InviteRepository inviteRepository;

    // ── Tạo server ────────────────────────────────────────────
    @Transactional
    public ServerResponse createServer(CreateServerRequest req, UUID ownerId) {
        Server server = Server.builder()
                .name(req.getName())
                .description(req.getDescription())
                .inviteCode(UUID.randomUUID().toString())
                .imageUrl(req.getImageUrl())
                .ownerId(ownerId)
                .build();
        server = serverRepository.save(server);

        // Tự động thêm owner vào danh sách thành viên
        ServerMember ownerMember = memberRepository.save(ServerMember.builder()
                .server(server)
                .userId(ownerId)
                .role(ServerMember.MemberRole.OWNER)
                .build());

        // Tạo channel "general" mặc định
        Channel generalChannel = channelRepository.save(Channel.builder()
                .name("general")
                .creatorId(ownerId)
                .type(Channel.ChannelType.TEXT)
                .server(server)
                .build());

        Channel voiceChannel = channelRepository.save(Channel.builder()
                .name("General Voice")
                .creatorId(ownerId)
                .type(Channel.ChannelType.VOICE)
                .server(server)
                .build());

        server.getMembers().add(ownerMember);
        server.getChannels().add(generalChannel);
        server.getChannels().add(voiceChannel);

        return toResponse(server);
    }

    // ── Lấy danh sách server của user ─────────────────────────
    public List<ServerResponse> getServersForUser(UUID userId) {
        return serverRepository.findByMembersUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Lấy chi tiết 1 server ─────────────────────────────────
    public ServerResponse getServerById(Long serverId, UUID userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Server not found"));

        if (!memberRepository.existsByServerIdAndUserId(serverId, userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You are not a member of this server");
        }

        return toResponse(server);
    }

    public List<ServerMemberResponse> getServerMembers(Long serverId, UUID requesterId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Server not found"));

        if (!memberRepository.existsByServerIdAndUserId(serverId, requesterId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You are not a member of this server");
        }

        return memberRepository.findByServerIdOrderByJoinedAtAsc(server.getId())
                .stream()
                .map(this::toServerMemberResponse)
                .toList();
    }

    // ── Tạo channel trong server ───────────────────────────────
    @Transactional
    public ChannelResponse createChannel(Long serverId, CreateChannelRequest req, UUID userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Server not found"));

        if (!server.getOwnerId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only the server owner can create channels");
        }

        Channel channel = channelRepository.save(Channel.builder()
                .name(req.getName())
                .creatorId(userId)
                .type(req.getType())
                .server(server)
                .build());

        return toChannelResponse(channel);
    }

    // ── Join server ────────────────────────────────────────────
    @Transactional
    public void joinServer(Long serverId, UUID userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Server not found"));

        if (memberRepository.existsByServerIdAndUserId(serverId, userId)) {
            throw new AppException(HttpStatus.CONFLICT, "You are already a member of this server");
        }

        memberRepository.save(ServerMember.builder()
                .server(server)
                .userId(userId)
                .role(ServerMember.MemberRole.GUEST)
                .build());
    }

    // ── Mappers ───────────────────────────────────────────────
    private ServerResponse toResponse(Server s) {
        return ServerResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .imageUrl(s.getImageUrl())
                .ownerId(s.getOwnerId())
                .memberCount(s.getMembers().size())
                .channels(s.getChannels().stream().map(this::toChannelResponse).toList())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private ChannelResponse toChannelResponse(Channel c) {
        return ChannelResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .type(c.getType().name())
                .serverId(c.getServer().getId())
                .build();
    }

    //Map ve object servermember reponse
    private ServerMemberResponse toServerMemberResponse(ServerMember member) {
        return new ServerMemberResponse(
                member.getId(),
                member.getServer().getId(),
                member.getUserId(),
                member.getRole().name(),
                member.getJoinedAt()
        );
    }


    //Kiem tra quyen get history, va nhan tin vao channel
    public boolean canAccessChannel(Long channelId, UUID userId) {
        // Channel access currently means: user is a member of the server owning this channel.
        Channel channel = channelRepository.findById(channelId)
        .orElseThrow(()-> new AppException(HttpStatus.NOT_FOUND, "Channel not found"));

        Long serverId = channel.getServer().getId();

        return memberRepository.existsByServerIdAndUserId(serverId, userId);
    }

    //Kiem tra quyen xoa tin nhan trong channel text
    public boolean hasChannelPermission(Long channelId, UUID userId, PermissionCode permission) {
        Channel channel = channelRepository.findById(channelId)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Channel not found"));

        ServerMember member = memberRepository.findByServerIdAndUserId(channel.getServer().getId(), userId)
        .orElseThrow(() -> new AppException(HttpStatus.FORBIDDEN, "You are not member of this channel"));

        return switch (member.getRole()) {
            case OWNER -> true;
            case ADMIN -> permission == PermissionCode.MANAGE_MESSAGES
                 || permission == PermissionCode.MANAGE_CHANNELS
                 || permission == PermissionCode.INVITE_MEMBERS;
            case MODERATOR -> permission == PermissionCode.MANAGE_MESSAGES
                || permission == PermissionCode.INVITE_MEMBERS;
            case GUEST -> false;
        };
    }
    
    @Transactional
    public ServerMemberResponse updateMemberRole(
        Long serverId,
        Long memberId,
        UUID requesterId,
        UpdateMemberRoleRequest req
    )   {
    ServerMember requester = memberRepository.findByServerIdAndUserId(serverId, requesterId)
            .orElseThrow(() -> new AppException(HttpStatus.FORBIDDEN, "You are not a member of this server"));

    if (requester.getRole() != ServerMember.MemberRole.OWNER) {
        throw new AppException(HttpStatus.FORBIDDEN, "Only server owner can update member roles");
    }

    ServerMember target = memberRepository.findByIdAndServerId(memberId, serverId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Member not found"));

    if (target.getRole() == ServerMember.MemberRole.OWNER) {
        throw new AppException(HttpStatus.BAD_REQUEST, "Cannot change owner role");
    }

    if (req.getRole() == ServerMember.MemberRole.OWNER) {
        throw new AppException(HttpStatus.BAD_REQUEST, "Cannot assign owner role");
    }

    target.setRole(req.getRole());
    ServerMember saved = memberRepository.save(target);

    return toServerMemberResponse(saved);
}

    @Transactional
    public void removeServerMember(Long serverId, Long memberId, UUID requesterId) {
    ServerMember requester = memberRepository.findByServerIdAndUserId(serverId, requesterId)
            .orElseThrow(() -> new AppException(HttpStatus.FORBIDDEN, "You are not a member of this server"));

    if (requester.getRole() != ServerMember.MemberRole.OWNER) {
        throw new AppException(HttpStatus.FORBIDDEN, "Only server owner can remove members");
    }

    ServerMember target = memberRepository.findByIdAndServerId(memberId, serverId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Member not found"));

    if (target.getRole() == ServerMember.MemberRole.OWNER) {
        throw new AppException(HttpStatus.BAD_REQUEST, "Cannot remove server owner");
    }

    if (target.getUserId().equals(requesterId)) {
        throw new AppException(HttpStatus.BAD_REQUEST, "Owner cannot remove themselves");
    }

    memberRepository.delete(target);
}


    //Logic update server
    @Transactional
    public ServerResponse updateServer(Long serverId, UUID requesterId, UpdateServerRequest req) {
    Server server = serverRepository.findById(serverId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Server not found"));

    if (!server.getOwnerId().equals(requesterId)) {
        throw new AppException(HttpStatus.FORBIDDEN, "Only server owner can update server");
    }

    if (req.getName() != null && !req.getName().isBlank()) {
        server.setName(req.getName().trim());
    }

    if (req.getDescription() != null) {
        server.setDescription(req.getDescription().isBlank() ? null : req.getDescription().trim());
    }

    if (req.getImageUrl() != null) {
        server.setImageUrl(req.getImageUrl().isBlank() ? null : req.getImageUrl().trim());
    }

    return toResponse(serverRepository.save(server));
}
    //Logic xoa server
    @Transactional
    public void deleteServer(Long serverId, UUID requesterId) {
    Server server = serverRepository.findById(serverId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Server not found"));

    if (!server.getOwnerId().equals(requesterId)) {
        throw new AppException(HttpStatus.FORBIDDEN, "Only server owner can delete server");
    }

    serverRepository.delete(server);
}  

    @Transactional
    public ChannelResponse updateChannel(Long serverId, Long channelId, UUID requesterId, UpdateChannelRequest req) {
    checkCanManageChannels(serverId, requesterId);

    Channel channel = channelRepository.findById(channelId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Channel not found"));

    if (!channel.getServer().getId().equals(serverId)) {
        throw new AppException(HttpStatus.BAD_REQUEST, "Channel does not belong to this server");
    }

    channel.setName(req.getName().trim());

    return toChannelResponse(channelRepository.save(channel));
}
    
    //Xoa kenh
    @Transactional
    public void deleteChannel(Long serverId, Long channelId, UUID requesterId) {
    checkCanManageChannels(serverId, requesterId);

    Channel channel = channelRepository.findById(channelId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Channel not found"));

    if (!channel.getServer().getId().equals(serverId)) {
        throw new AppException(HttpStatus.BAD_REQUEST, "Channel does not belong to this server");
    }

    channelRepository.delete(channel);
}


    private void checkCanManageChannels(Long serverId, UUID requesterId) {
    ServerMember member = memberRepository.findByServerIdAndUserId(serverId, requesterId)
            .orElseThrow(() -> new AppException(HttpStatus.FORBIDDEN, "You are not a member of this server"));

    if (member.getRole() != ServerMember.MemberRole.OWNER) {
        throw new AppException(HttpStatus.FORBIDDEN, "Only server owner can manage channels");
    }
    }

}
