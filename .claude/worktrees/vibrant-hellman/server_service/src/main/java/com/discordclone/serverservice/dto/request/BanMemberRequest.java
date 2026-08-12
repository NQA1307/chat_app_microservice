package com.discordclone.serverservice.dto.request;

import java.util.UUID;

import lombok.Data;

@Data
public class BanMemberRequest {
    private UUID userId;
    private String reason;
}


