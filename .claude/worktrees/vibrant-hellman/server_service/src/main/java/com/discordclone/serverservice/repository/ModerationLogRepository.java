package com.discordclone.serverservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.discordclone.serverservice.entity.ModerationLog;

public interface ModerationLogRepository extends JpaRepository<ModerationLog, Long> {
    List<ModerationLog> findByServerIdOrderByCreatedAtDesc(Long serverId);
    void deleteByServerId(Long serverId);
}