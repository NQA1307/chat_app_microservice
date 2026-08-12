package com.discordclone.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetConfirmRequest {
    @NotBlank
    private String resetToken;

    @NotBlank
    @Size(min = 8, max = 72)
    private String newPassword;
}
