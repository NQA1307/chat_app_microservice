package com.discordclone.messageservice.dto.request;

import lombok.Data;

@Data
public class MarkChannelReadRequest {
    private String messageId;
}