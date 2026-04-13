package com.discordclone.serverservice.service;

import com.discordclone.common.exception.AppException;
import com.discordclone.serverservice.dto.*;
import com.discordclone.serverservice.entity.*;
import com.discordclone.serverservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServerService {

    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    private final ServerMemberRepository memberRepository;

    // ── Tạo server ────────────────────────────────────────────
    @Transactional
    public ServerResponse createServer(CreateServerRequest req, Long ownerId) {
        Server server = Server.builder()
                .name(req.getName())
                .description(req.getDescription())
                .imageUrl(req.getImageUrl())
                .ownerId(ownerId)
                .build();
        server = serverRepository.save(server);

        // Tự động thêm owner vào danh sách thành viên
        memberRepository.save(ServerMember.builder()
                .server(server)
                .userId(ownerId)
                .role(ServerMember.MemberRole.OWNER)
                .build());

        // Tạo channel "general" mặc định
        channelRepository.save(Channel.builder()
                .name("general")
                .type(Channel.ChannelType.TEXT)
                .server(server)
                .build());

        return toResponse(serverRepository.findById(server.getId()).orElseThrow());
    }

    // ── Lấy danh sách server của user ─────────────────────────
    public List<ServerResponse> getServersForUser(Long userId) {
        return serverRepository.findByMembersUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Lấy chi tiết 1 server ─────────────────────────────────
    public ServerResponse getServerById(Long serverId, Long userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Server not found"));

        if (!memberRepository.existsByServerIdAndUserId(serverId, userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You are not a member of this server");
        }

        return toResponse(server);
    }

    // ── Tạo channel trong server ───────────────────────────────
    @Transactional
    public ChannelResponse createChannel(Long serverId, CreateChannelRequest req, Long userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Server not found"));

        if (!server.getOwnerId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only the server owner can create channels");
        }

        Channel channel = channelRepository.save(Channel.builder()
                .name(req.getName())
                .type(req.getType())
                .server(server)
                .build());

        return toChannelResponse(channel);
    }

    // ── Join server ────────────────────────────────────────────
    @Transactional
    public void joinServer(Long serverId, Long userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Server not found"));

        if (memberRepository.existsByServerIdAndUserId(serverId, userId)) {
            throw new AppException(HttpStatus.CONFLICT, "You are already a member of this server");
        }

        memberRepository.save(ServerMember.builder()
                .server(server)
                .userId(userId)
                .role(ServerMember.MemberRole.MEMBER)
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
}
