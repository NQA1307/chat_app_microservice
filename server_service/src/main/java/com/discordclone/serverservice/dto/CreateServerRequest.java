package com.discordclone.serverservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateServerRequest {

    @NotBlank(message = "Server name is required")
    private String name;

    private String description;

    private String imageUrl;
}
