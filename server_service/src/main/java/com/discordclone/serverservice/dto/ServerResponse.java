package com.discordclone.serverservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ServerResponse {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private UUID ownerId;
    private int memberCount;
    private List<ChannelResponse> channels;
    private LocalDateTime createdAt;
}
