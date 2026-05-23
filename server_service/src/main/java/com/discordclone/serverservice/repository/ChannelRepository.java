package com.discordclone.serverservice.repository;

import com.discordclone.serverservice.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {

    List<Channel> findByServerId(Long serverId);

    Optional<Channel> findById(Long id);
}
