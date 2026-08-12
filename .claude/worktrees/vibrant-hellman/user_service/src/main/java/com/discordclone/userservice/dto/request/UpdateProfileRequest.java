package com.discordclone.userservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Display name must be between 2 and 50 characters")
    private String displayName;

    private String bio;

    @Size(max = 7)
    private String bannerColor;

    private String customStatus;
}
