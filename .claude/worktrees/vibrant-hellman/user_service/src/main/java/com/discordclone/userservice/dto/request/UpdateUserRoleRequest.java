package com.discordclone.userservice.dto.request;

import com.discordclone.userservice.entity.User;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "Role is required")
        User.Role role,
        
        String reason
) {}
