package com.discordclone.serverservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.discordclone.serverservice.entity.ServerBan;

public interface ServerBanRepository extends JpaRepository<ServerBan, Long> {

    boolean existsByServerIdAndUserId(Long serverId, UUID userId);
    Optional<ServerBan> findByServerIdAndUserId(Long serverId, UUID userId);
    void deleteByServerIdAndUserId(Long serverId, UUID userId);
    void deleteByServerId(Long serverId);
}
