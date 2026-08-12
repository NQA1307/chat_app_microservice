package com.discordclone.serverservice.service;

import com.discordclone.common.enums.ModerationAction;
import com.discordclone.common.enums.MuteType;
import com.discordclone.common.enums.PermissionCode;
import com.discordclone.common.exception.AppException;
import com.discordclone.serverservice.dto.request.CreateChannelRequest;
import com.discordclone.serverservice.dto.request.CreateServerRequest;
import com.discordclone.serverservice.dto.request.MuteMemberRequest;
import com.discordclone.serverservice.dto.request.UpdateChannelRequest;
import com.discordclone.serverservice.dto.request.UpdateMemberRoleRequest;
import com.discordclone.serverservice.dto.request.UpdateServerRequest;
import com.discordclone.serverservice.dto.response.ChannelResponse;
import com.discordclone.serverservice.dto.response.ModerationLogResponse;
import com.discordclone.serverservice.dto.response.ServerMemberResponse;
import com.discordclone.serverservice.dto.response.ServerResponse;
import com.discordclone.serverservice.entity.*;
import com.discordclone.serverservice.entity.ServerMember.MemberRole;
import com.discordclone.serverservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServerService {

    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    private final ServerMemberRepository memberRepository;
    private final InviteRepository inviteRepository;
    private final ServerBanRepository   serverBanRepository;
    private final ServerMuteRepository serverMuteRepository;
    private final ModerationLogRepository moderationLogRepository;
    private final DomainEventPublisher eventPublisher;

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
    public List<ServerMemberResponse> searchServerMembers(Long serverId, UUID requesterId, String query, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 25));
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();

        return getServerMembers(serverId, requesterId).stream()
                .filter(member -> normalizedQuery.isBlank()
                        || member.userId().toString().toLowerCase().contains(normalizedQuery)
                        || member.role().toLowerCase().contains(normalizedQuery))
                .limit(safeLimit)
                .toList();
    }

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

        if (serverBanRepository.existsByServerIdAndUserId(serverId, userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You are banned from this server");
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

        return memberRepository.existsByServerIdAndUserId(serverId, userId)
            && !serverBanRepository.existsByServerIdAndUserId(serverId, userId);
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
                 || permission == PermissionCode.INVITE_MEMBERS
                 || permission == PermissionCode.KICK_MEMBERS
                 || permission == PermissionCode.BAN_MEMBERS
                 || permission == PermissionCode.MUTE_MEMBERS
                 || permission == PermissionCode.ATTACH_FILES
                 || permission == PermissionCode.SEND_MESSAGES
                 || permission == PermissionCode.CONNECT_VOICE
                 || permission == PermissionCode.SPEAK
                 || permission == PermissionCode.MENTION_EVERYONE;
            case MODERATOR -> permission == PermissionCode.MANAGE_MESSAGES
                || permission == PermissionCode.INVITE_MEMBERS
                || permission == PermissionCode.KICK_MEMBERS
                || permission == PermissionCode.MUTE_MEMBERS
                || permission == PermissionCode.ATTACH_FILES
                || permission == PermissionCode.SEND_MESSAGES
                || permission == PermissionCode.CONNECT_VOICE
                || permission == PermissionCode.SPEAK
                || permission == PermissionCode.MENTION_EVERYONE;
            case GUEST -> permission == PermissionCode.ATTACH_FILES
                || permission == PermissionCode.SEND_MESSAGES
                || permission == PermissionCode.CONNECT_VOICE
                || permission == PermissionCode.SPEAK;
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

    private void requirePermission(Long serverId, UUID actorId, PermissionCode permission) {
    ServerMember actor = memberRepository
            .findByServerIdAndUserId(serverId, actorId)
            .orElseThrow(() -> new AppException(HttpStatus.FORBIDDEN, "You are not a member of this server"));

    boolean allowed = switch (actor.getRole()) {
        case OWNER -> true;
        case ADMIN -> permission == PermissionCode.KICK_MEMBERS
                || permission == PermissionCode.BAN_MEMBERS
                || permission == PermissionCode.MUTE_MEMBERS
                || permission == PermissionCode.MANAGE_MESSAGES
                || permission == PermissionCode.MANAGE_CHANNELS
                || permission == PermissionCode.INVITE_MEMBERS
                || permission == PermissionCode.ATTACH_FILES
                || permission == PermissionCode.SEND_MESSAGES
                || permission == PermissionCode.CONNECT_VOICE
                || permission == PermissionCode.SPEAK
                || permission == PermissionCode.MENTION_EVERYONE;
        case MODERATOR -> permission == PermissionCode.KICK_MEMBERS
                || permission == PermissionCode.MUTE_MEMBERS
                || permission == PermissionCode.MANAGE_MESSAGES
                || permission == PermissionCode.INVITE_MEMBERS
                || permission == PermissionCode.ATTACH_FILES
                || permission == PermissionCode.SEND_MESSAGES
                || permission == PermissionCode.CONNECT_VOICE
                || permission == PermissionCode.SPEAK
                || permission == PermissionCode.MENTION_EVERYONE;
        case GUEST -> permission == PermissionCode.ATTACH_FILES
                || permission == PermissionCode.SEND_MESSAGES
                || permission == PermissionCode.CONNECT_VOICE
                || permission == PermissionCode.SPEAK;
    };

    if (!allowed) {
        throw new AppException(HttpStatus.FORBIDDEN, "Missing permission: " + permission);
    }
}


    //Kick member khoi server
    @Transactional
    public void kickMember(Long serverId, UUID targetUserId, UUID actorId, String reason) {
        requirePermission(serverId, actorId, PermissionCode.KICK_MEMBERS);

        if (actorId.equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "You cannot kick yourself");
        }

        ServerMember target = memberRepository.findByServerIdAndUserId(serverId, targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Member not found"));

        if (target.getRole() == MemberRole.OWNER) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot kick server owner");
        }

        memberRepository.delete(target);

        writeModerationLog(serverId, actorId, targetUserId, ModerationAction.KICK_MEMBER, reason);
    }

    //Ban member khoi server
    @Transactional
    public void banMember(Long serverId, UUID targetUserId, UUID actorId, String reason) {
        requirePermission(serverId, actorId, PermissionCode.BAN_MEMBERS);

        if (actorId.equals(targetUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "You cannot ban yourself");
        }

        memberRepository.findByServerIdAndUserId(serverId, targetUserId)
                .ifPresent(member -> {
                    if (member.getRole() == MemberRole.OWNER) {
                        throw new AppException(HttpStatus.BAD_REQUEST, "Cannot ban server owner");
                    }
                    memberRepository.delete(member);
                });

        if (!serverBanRepository.existsByServerIdAndUserId(serverId, targetUserId)) {
            serverBanRepository.save(ServerBan.builder()
                    .serverId(serverId)
                    .userId(targetUserId)
                    .bannedBy(actorId)
                    .reason(reason)
                    .build());
        }

        writeModerationLog(serverId, actorId, targetUserId, ModerationAction.BAN_MEMBER, reason);
    }


    //Unban member
    @Transactional
    public void unbanMember(Long serverId, UUID targetUserId, UUID actorId, String reason) {
        requirePermission(serverId, actorId, PermissionCode.BAN_MEMBERS);

        serverBanRepository.deleteByServerIdAndUserId(serverId, targetUserId);

        writeModerationLog(serverId, actorId, targetUserId, ModerationAction.UNBAN_MEMBER, reason);
    }


    //Mute khoi kenh
    @Transactional
    public void muteMember(Long serverId, UUID actorId, MuteMemberRequest req) {
        requirePermission(serverId, actorId, PermissionCode.MUTE_MEMBERS);

        if (actorId.equals(req.getUserId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "You cannot mute yourself");
        }

        ServerMember target = memberRepository.findByServerIdAndUserId(serverId, req.getUserId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Member not found"));

        if (target.getRole() == MemberRole.OWNER) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot mute server owner");
        }

        serverMuteRepository.save(ServerMute.builder()
                .serverId(serverId)
                .channelId(req.getChannelId())
                .userId(req.getUserId())
                .mutedBy(actorId)
                .type(req.getType())
                .expiresAt(req.getExpiresAt())
                .reason(req.getReason())
                .build());

        writeModerationLog(serverId, actorId, req.getUserId(), ModerationAction.MUTE_MEMBER, req.getReason());
} 


    //Unmute member
    @Transactional
    public void unmuteMember(Long serverId, UUID targetUserId, UUID actorId, String reason) {
        requirePermission(serverId, actorId, PermissionCode.MUTE_MEMBERS);

        serverMuteRepository.deleteByServerIdAndUserId(serverId, targetUserId);

        writeModerationLog(serverId, actorId, targetUserId, ModerationAction.UNMUTE_MEMBER, reason);
    }

    public boolean canSendMessage(Long channelId, UUID userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Channel not found"));

        Long serverId = channel.getServer().getId();

        if (!canAccessChannel(channelId, userId)) {
            return false;
        }

        List<ServerMute> activeMutes = serverMuteRepository.findActiveMutes(
                serverId,
                channelId,
                userId,
                LocalDateTime.now());

        boolean muted = activeMutes.stream().anyMatch(mute ->
                mute.getType() == MuteType.TEXT || mute.getType() == MuteType.ALL);

        return !muted && hasChannelPermission(channelId, userId, PermissionCode.SEND_MESSAGES);
    }

    public boolean canUploadAttachment(Long channelId, UUID userId) {
        return canSendMessage(channelId, userId)
                && hasChannelPermission(channelId, userId, PermissionCode.ATTACH_FILES);
    }

    public boolean canJoinVoice(Long channelId, UUID userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Channel not found"));

        Long serverId = channel.getServer().getId();

        if (!canAccessChannel(channelId, userId)) {
            return false;
        }

        List<ServerMute> activeMutes = serverMuteRepository.findActiveMutes(
                serverId,
                channelId,
                userId,
                LocalDateTime.now());

        boolean muted = activeMutes.stream().anyMatch(mute ->
                mute.getType() == MuteType.VOICE || mute.getType() == MuteType.ALL);

        return !muted && hasChannelPermission(channelId, userId, PermissionCode.CONNECT_VOICE);
    }

    public List<ModerationLogResponse> getModerationLogs(Long serverId, UUID requesterId) {
        requirePermission(serverId, requesterId, PermissionCode.MUTE_MEMBERS);

        return moderationLogRepository.findByServerIdOrderByCreatedAtDesc(serverId)
                .stream()
                .map(this::toModerationLogResponse)
                .toList();
    }


    //Viet log
    private void writeModerationLog(
            Long serverId,
            UUID actorId,
            UUID targetUserId,
            ModerationAction action,
            String reason
    ) {
        moderationLogRepository.save(ModerationLog.builder()
                .serverId(serverId)
                .actorId(actorId)
                .targetUserId(targetUserId)
                .action(action)
                .reason(reason)
                .build());
    }

    private ModerationLogResponse toModerationLogResponse(ModerationLog log) {
        return ModerationLogResponse.builder()
                .id(log.getId())
                .serverId(log.getServerId())
                .actorId(log.getActorId())
                .targetUserId(log.getTargetUserId())
                .action(log.getAction())
                .reason(log.getReason())
                .createdAt(log.getCreatedAt())
                .build();
    }


    @Transactional
    public void leaveServer(Long serverId, UUID userId) {
        Server server = serverRepository.findById(serverId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Server not found"));

        ServerMember member = memberRepository.findByServerIdAndUserId(serverId, userId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "You are not a member of this server"));

        if (server.getOwnerId().equals(userId) || member.getRole() == MemberRole.OWNER) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Owner cannot leave server. Transfer ownership or delete server first");
        }

        memberRepository.delete(member);

        eventPublisher.publish(
                com.discordclone.common.event.EventTypes.SERVER_MEMBER_LEFT,
                "server.member.left",
                new com.discordclone.common.event.ServerMemberEvent(
                        serverId,
                        server.getName(),
                        userId,
                        userId,
                        "LEFT"
                )
        );

        // Defensive guard to clean up server if no members are left
        if (memberRepository.countByServerId(serverId) == 0) {
            cleanupAndDeleteServer(serverId);
        }
    }

    private void cleanupAndDeleteServer(Long serverId) {
        inviteRepository.deleteByTargetTypeAndTargetId("SERVER", String.valueOf(serverId));
        serverBanRepository.deleteByServerId(serverId);
        serverMuteRepository.deleteByServerId(serverId);
        moderationLogRepository.deleteByServerId(serverId);
        serverRepository.deleteById(serverId);
    }
}
