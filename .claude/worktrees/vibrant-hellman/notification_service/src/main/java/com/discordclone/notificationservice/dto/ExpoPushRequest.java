package com.discordclone.notificationservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ExpoPushRequest {
    private String to;
    private String title;
    private String body;
    private String sound;
    private int badge;
    private Map<String, Object> data;
}
