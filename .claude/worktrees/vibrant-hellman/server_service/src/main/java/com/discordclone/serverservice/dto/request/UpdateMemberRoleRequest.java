package com.discordclone.serverservice.dto.request;

import com.discordclone.serverservice.entity.ServerMember.MemberRole;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMemberRoleRequest {
    @NotNull
    private MemberRole role;
}
