package com.discordclone.serverservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.serverservice.repository.ServerMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/servers")
@RequiredArgsConstructor
public class InternalServerController {

    private final ServerMemberRepository memberRepository;

    @GetMapping("/users/{userId}/co-member-ids")
    public ResponseEntity<ApiResponse<List<UUID>>> getCoMemberIds(@PathVariable("userId") UUID userId) {
        List<UUID> coMemberIds = memberRepository.findCoMemberIdsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(coMemberIds));
    }

    @GetMapping("/channels/{channelId}/member-ids")
    public ResponseEntity<ApiResponse<List<UUID>>> getChannelMemberIds(@PathVariable("channelId") Long channelId) {
        List<UUID> memberIds = memberRepository.findUserIdsByChannelId(channelId);
        return ResponseEntity.ok(ApiResponse.success(memberIds));
    }
}
