package com.discordclone.serverservice.repository;

import com.discordclone.serverservice.entity.ServerMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServerMemberRepository extends JpaRepository<ServerMember, Long> {

    boolean existsByServerIdAndUserId(Long serverId, Long userId);

    Optional<ServerMember> findByServerIdAndUserId(Long serverId, Long userId);
}
