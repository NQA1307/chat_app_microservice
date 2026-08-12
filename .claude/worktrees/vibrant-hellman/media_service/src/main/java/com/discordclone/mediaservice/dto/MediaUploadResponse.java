package com.discordclone.mediaservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MediaUploadResponse {
    private String id;
    private String originalFilename;
    private String contentType;
    private long size;
    private String url;
}
