package com.discordclone.serverservice.dto;

import com.discordclone.serverservice.entity.Channel;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateChannelRequest {

    @NotBlank(message = "Channel name is required")
    private String name;

    private Channel.ChannelType type = Channel.ChannelType.TEXT;
}
