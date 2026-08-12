package com.discordclone.messageservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.messageservice.dto.request.SetReactionRequest;
import com.discordclone.messageservice.dto.response.ReactionUpdateResponse;
import com.discordclone.messageservice.service.MessageReactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MessageReactionController {

    private final MessageReactionService reactionService;

    @PutMapping("/api/messages/{messageId}/reaction")
    public ApiResponse<ReactionUpdateResponse> setChannelReaction(
            @PathVariable("messageId") String messageId,
            @Valid @RequestBody SetReactionRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                reactionService.setChannelReaction(messageId, currentUserId(httpRequest), request)
        );
    }

    @DeleteMapping("/api/messages/{messageId}/reaction")
    public ApiResponse<ReactionUpdateResponse> removeChannelReaction(
            @PathVariable("messageId") String messageId,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                reactionService.removeChannelReaction(messageId, currentUserId(httpRequest))
        );
    }

    @PutMapping("/api/dm/messages/{messageId}/reaction")
    public ApiResponse<ReactionUpdateResponse> setDirectReaction(
            @PathVariable("messageId") String messageId,
            @Valid @RequestBody SetReactionRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                reactionService.setDirectReaction(messageId, currentUserId(httpRequest), request)
        );
    }

    @DeleteMapping("/api/dm/messages/{messageId}/reaction")
    public ApiResponse<ReactionUpdateResponse> removeDirectReaction(
            @PathVariable("messageId") String messageId,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                reactionService.removeDirectReaction(messageId, currentUserId(httpRequest))
        );
    }

    private UUID currentUserId(HttpServletRequest request) {
        return UUID.fromString(request.getHeader("X-User-Id"));
    }
}
