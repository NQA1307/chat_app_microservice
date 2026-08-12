package com.discordclone.serverservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.discordclone.serverservice.entity.ServerMute;

public interface ServerMuteRepository extends JpaRepository<ServerMute, Long> {
    List<ServerMute> findByServerIdAndUserId(Long serverId, UUID userId);

    @Query("""
        select m from ServerMute m
        where m.serverId = :serverId
          and m.userId = :userId
          and (m.channelId is null or m.channelId = :channelId)
          and (m.expiresAt is null or m.expiresAt > :now)
    """)
    List<ServerMute> findActiveMutes(
        @Param("serverId") Long serverId,
        @Param("channelId") Long channelId,
        @Param("userId") UUID userId,
        @Param("now") LocalDateTime now
    );

    void deleteByServerIdAndUserId(Long serverId, UUID userId);
    void deleteByServerId(Long serverId);
}
