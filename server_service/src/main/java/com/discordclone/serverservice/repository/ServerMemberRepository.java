package com.discordclone.serverservice.repository;

import com.discordclone.serverservice.entity.ServerMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServerMemberRepository extends JpaRepository<ServerMember, Long> {

    boolean existsByServerIdAndUserId(Long serverId, UUID userId);

    Optional<ServerMember> findByServerIdAndUserId(Long serverId, UUID userId);

    List<ServerMember> findByServerIdOrderByJoinedAtAsc(Long serverId);

    Optional<ServerMember> findByIdAndServerId(Long id, Long serverId);
}
