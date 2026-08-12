package com.discordclone.serverservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.common.enums.PermissionCode;
import com.discordclone.serverservice.dto.request.BanMemberRequest;
import com.discordclone.serverservice.dto.request.CreateChannelRequest;
import com.discordclone.serverservice.dto.request.CreateServerRequest;
import com.discordclone.serverservice.dto.request.MuteMemberRequest;
import com.discordclone.serverservice.dto.request.SendServerInviteRequest;
import com.discordclone.serverservice.dto.request.UpdateChannelRequest;
import com.discordclone.serverservice.dto.request.UpdateMemberRoleRequest;
import com.discordclone.serverservice.dto.request.UpdateServerRequest;
import com.discordclone.serverservice.dto.response.ChannelResponse;
import com.discordclone.serverservice.dto.response.InviteResponse;
import com.discordclone.serverservice.dto.response.ModerationLogResponse;
import com.discordclone.serverservice.dto.response.ServerMemberResponse;
import com.discordclone.serverservice.dto.response.ServerResponse;
import com.discordclone.serverservice.service.InviteService;
import com.discordclone.serverservice.service.ServerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;




@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;
    private final InviteService inviteService;

    /**
     * Đọc userId từ header X-User-Id được API Gateway forward sau khi validate JWT.
     */
    private UUID getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            throw new RuntimeException("Missing X-User-Id header");
        }
        return UUID.fromString(userId);
    }

    // POST /api/servers — Tạo server mới
    @PostMapping
    public ResponseEntity<ApiResponse<ServerResponse>> createServer(
            @Valid @RequestBody CreateServerRequest req,
            HttpServletRequest request) {
        ServerResponse server = serverService.createServer(req, getUserId(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Server created", server));
    }

    // GET /api/servers — Lấy danh sách server mà user tham gia
    @GetMapping
    public ResponseEntity<ApiResponse<List<ServerResponse>>> getMyServers(HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(serverService.getServersForUser(getUserId(request))));
    }

    // GET /api/servers/{id} — Chi tiết 1 server
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServerResponse>> getServer(
            @PathVariable("id") Long id,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(serverService.getServerById(id, getUserId(request))));
    }

    // POST /api/servers/{id}/channels — Tạo channel (chỉ owner)
    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<ServerMemberResponse>>> getServerMembers(
            @PathVariable("id") Long id,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(serverService.getServerMembers(id, getUserId(request))));
    }

    @GetMapping("/{id}/members/search")
    public ResponseEntity<ApiResponse<List<ServerMemberResponse>>> searchServerMembers(
            @PathVariable("id") Long id,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(serverService.searchServerMembers(id, getUserId(request), query, limit)));
    }

    @PostMapping("/{id}/channels")
    public ResponseEntity<ApiResponse<ChannelResponse>> createChannel(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateChannelRequest req,
            HttpServletRequest request) {
        ChannelResponse channel = serverService.createChannel(id, req, getUserId(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Channel created", channel));
    }

    // POST /api/servers/{id}/join — Join server
    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<Void>> joinServer(
            @PathVariable("id") Long id,
            HttpServletRequest request) {
        serverService.joinServer(id, getUserId(request));
        return ResponseEntity.ok(ApiResponse.success("Joined server successfully", null));
    }

    //Gửi đi lời mời
    @PostMapping("/{serverId}/invites")
    public ResponseEntity<ApiResponse<InviteResponse>> sendServerInvite(
        @PathVariable("serverId") Long serverId,
        @RequestBody SendServerInviteRequest request,
        HttpServletRequest httpRequest
    ) {
        InviteResponse inivite = inviteService.sendServerInvite(
            serverId,
            getUserId(httpRequest),
             request);
        
             return ResponseEntity.status(HttpStatus.CREATED)
             .body(ApiResponse.success("Invite sent", inivite));
        }

    //Lay danh sach loi moi
    @GetMapping("/invites/incoming")
    public ResponseEntity<ApiResponse<List<InviteResponse>>> getIncomingInvites(
        HttpServletRequest request
    ) {
        List<InviteResponse> invites = inviteService.getIncomingInvites(getUserId(request));
        return ResponseEntity.ok(ApiResponse.success(invites));
    }
    //Tu choi invite to server
    @PostMapping("/invites/{inviteId}/decline")
    public ResponseEntity<ApiResponse<Void>> declineInvite (
        @PathVariable("inviteId") String inviteId,
        HttpServletRequest request
    ) {
        inviteService.declineInvite(inviteId, getUserId(request));
        return ResponseEntity.ok(ApiResponse.success("Invite declined", null));
    }

    //Chấp nhận lời mời tham gia server
    @PostMapping("/invites/{inviteId}/accept")
    public ResponseEntity<ApiResponse<ServerResponse>> acceptInvite(
        @PathVariable("inviteId") String inviteId,
        HttpServletRequest request
) {
    ServerResponse server = inviteService.acceptInvite(inviteId, getUserId(request));
    return ResponseEntity.ok(ApiResponse.success("Invite accepted", server));
}

    //kiem tra quyen access vao 1 channel
    @GetMapping("/channels/{channelId}/access")
    public ResponseEntity<ApiResponse<Boolean>> canAccessChannel(
        @PathVariable("channelId")  Long channelId,
        @RequestParam("userId") UUID userId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(serverService.canAccessChannel(channelId, userId))
        );
    }
    
    @GetMapping("/channels/{channelId}/permissions/{permission}")
    public ResponseEntity<ApiResponse<Boolean>> hasChannelPermission(
        @PathVariable("channelId") Long channelId,
        @PathVariable("permission") PermissionCode permission,
        @RequestParam("userId") UUID userId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(serverService.hasChannelPermission(channelId, userId, permission))
        );
    }

    @GetMapping("/channels/{channelId}/can-send-message")
    public ResponseEntity<ApiResponse<Boolean>> canSendMessage(
        @PathVariable("channelId") Long channelId,
        @RequestParam("userId") UUID userId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(serverService.canSendMessage(channelId, userId))
        );
    }

    @GetMapping("/channels/{channelId}/can-upload-attachment")
    public ResponseEntity<ApiResponse<Boolean>> canUploadAttachment(
        @PathVariable("channelId") Long channelId,
        @RequestParam("userId") UUID userId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(serverService.canUploadAttachment(channelId, userId))
        );
    }

    @GetMapping("/channels/{channelId}/can-join-voice")
    public ResponseEntity<ApiResponse<Boolean>> canJoinVoice(
        @PathVariable("channelId") Long channelId,
        @RequestParam("userId") UUID userId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(serverService.canJoinVoice(channelId, userId))
        );
    }

    //Owner phan quyen cho user trong server
    @PatchMapping("/{serverId}/members/{memberId}/role")
    public ResponseEntity<ApiResponse<ServerMemberResponse>> updateMemberRole(
        @PathVariable("serverId") Long serverId,
        @PathVariable("memberId") Long memberId,
        @Valid @RequestBody UpdateMemberRoleRequest request,
        HttpServletRequest httpRequest) {
    ServerMemberResponse member = serverService.updateMemberRole(
            serverId,
            memberId,
            getUserId(httpRequest),
            request
    );

    return ResponseEntity.ok(ApiResponse.success("Member role updated", member));
    }

    @DeleteMapping("/{serverId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeServerMember(
        @PathVariable("serverId") Long serverId,
        @PathVariable("memberId") Long memberId,
        HttpServletRequest request) {
    serverService.removeServerMember(serverId, memberId, getUserId(request));
    return ResponseEntity.ok(ApiResponse.success("Member removed", null));
    }

    @PostMapping("/{serverId}/members/{userId}/kick")
    public ResponseEntity<ApiResponse<Void>> kickMember(
        @PathVariable("serverId") Long serverId,
        @PathVariable("userId") UUID userId,
        @RequestBody(required = false) BanMemberRequest request,
        HttpServletRequest httpRequest
    ) {
        serverService.kickMember(
            serverId,
            userId,
            getUserId(httpRequest),
            request == null ? null : request.getReason()
        );
        return ResponseEntity.ok(ApiResponse.success("Member kicked", null));
    }

    @PostMapping("/{serverId}/bans")
    public ResponseEntity<ApiResponse<Void>> banMember(
        @PathVariable("serverId") Long serverId,
        @RequestBody BanMemberRequest request,
        HttpServletRequest httpRequest
    ) {
        serverService.banMember(
            serverId,
            request.getUserId(),
            getUserId(httpRequest),
            request.getReason()
        );
        return ResponseEntity.ok(ApiResponse.success("Member banned", null));
    }

    @DeleteMapping("/{serverId}/bans/{userId}")
    public ResponseEntity<ApiResponse<Void>> unbanMember(
        @PathVariable("serverId") Long serverId,
        @PathVariable("userId") UUID userId,
        HttpServletRequest httpRequest
    ) {
        serverService.unbanMember(serverId, userId, getUserId(httpRequest), null);
        return ResponseEntity.ok(ApiResponse.success("Member unbanned", null));
    }

    @PostMapping("/{serverId}/mutes")
    public ResponseEntity<ApiResponse<Void>> muteMember(
        @PathVariable("serverId") Long serverId,
        @RequestBody MuteMemberRequest request,
        HttpServletRequest httpRequest
    ) {
        serverService.muteMember(serverId, getUserId(httpRequest), request);
        return ResponseEntity.ok(ApiResponse.success("Member muted", null));
    }

    @DeleteMapping("/{serverId}/mutes/{userId}")
    public ResponseEntity<ApiResponse<Void>> unmuteMember(
        @PathVariable("serverId") Long serverId,
        @PathVariable("userId") UUID userId,
        HttpServletRequest httpRequest
    ) {
        serverService.unmuteMember(serverId, userId, getUserId(httpRequest), null);
        return ResponseEntity.ok(ApiResponse.success("Member unmuted", null));
    }

    @GetMapping("/{serverId}/moderation-logs")
    public ResponseEntity<ApiResponse<List<ModerationLogResponse>>> getModerationLogs(
        @PathVariable("serverId") Long serverId,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(serverService.getModerationLogs(serverId, getUserId(httpRequest)))
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ServerResponse>> updateServer(
        @PathVariable("id") Long id,
        @Valid @RequestBody UpdateServerRequest req,
        HttpServletRequest request) {
    ServerResponse server = serverService.updateServer(id, getUserId(request), req);
    return ResponseEntity.ok(ApiResponse.success("Server updated", server));
}


    //Xoa server
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteServer(
        @PathVariable("id") Long id,
        HttpServletRequest request) {
    serverService.deleteServer(id, getUserId(request));
    return ResponseEntity.ok(ApiResponse.success("Server deleted", null));
}
    //Cap nhat Channel
    @PatchMapping("/{serverId}/channels/{channelId}")
    public ResponseEntity<ApiResponse<ChannelResponse>> updateChannel(
            @PathVariable("serverId") Long serverId,
            @PathVariable("channelId") Long channelId,
            @Valid @RequestBody UpdateChannelRequest req,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Channel updated",
                        serverService.updateChannel(serverId, channelId, getUserId(request), req)));
    }


    //Xoa channel
    @DeleteMapping("/{serverId}/channels/{channelId}")
    public ResponseEntity<ApiResponse<Void>> deleteChannel(
        @PathVariable("serverId") Long serverId,
        @PathVariable("channelId") Long channelId,
        HttpServletRequest request) {
    serverService.deleteChannel(serverId, channelId, getUserId(request));
    return ResponseEntity.ok(ApiResponse.success("Channel deleted", null));
}


//Roi khoi server
    @DeleteMapping("/{serverId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveServer(
        @PathVariable("serverId") Long serverId,
        HttpServletRequest request
    ) {
        serverService.leaveServer(serverId, getUserId(request));
        return ResponseEntity.ok(ApiResponse.success("Left server", null));
    }
}
