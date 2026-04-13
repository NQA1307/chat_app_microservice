package com.discordclone.serverservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ServerResponse {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Long ownerId;
    private int memberCount;
    private List<ChannelResponse> channels;
    private LocalDateTime createdAt;
}
