package com.discordclone.userservice.dto.response;

public record AdminDashboardResponse(
        long totalUsers,
        long verifiedUsers,
        long bannedUsers,
        long lockedUsers,
        long usersCreatedToday
) {}
