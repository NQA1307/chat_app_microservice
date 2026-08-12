package com.discordclone.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipResponse {
    private UUID targetUserId;
    private String status; // FRIEND, PENDING_INCOMING, PENDING_OUTGOING, BLOCKED_BY_ME, BLOCKED_ME, NONE
}
