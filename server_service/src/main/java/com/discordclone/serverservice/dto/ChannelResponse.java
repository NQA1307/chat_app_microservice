package com.discordclone.serverservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChannelResponse {

    private Long id;
    private String name;
    private String type;
    private Long serverId;
}
