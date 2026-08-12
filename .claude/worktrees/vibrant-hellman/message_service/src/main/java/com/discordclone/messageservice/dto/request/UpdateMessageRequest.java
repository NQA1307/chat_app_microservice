package com.discordclone.messageservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMessageRequest {

    @NotBlank(message = "Content cannot be empty")
    private String content;
}