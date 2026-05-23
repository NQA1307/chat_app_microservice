package com.discordclone.serverservice.dto;

import com.discordclone.serverservice.entity.Channel;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateChannelRequest {
    @NotBlank
    private String name;

}
