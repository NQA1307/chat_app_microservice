package com.discordclone.serverservice.repository;

import com.discordclone.serverservice.entity.ServerMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    long countByServerId(Long serverId);

    @Query("""
        select distinct m.userId from ServerMember m
        where m.server.id in (
            select sub.server.id from ServerMember sub where sub.userId = :userId
        )
        and m.userId <> :userId
    """)
    List<UUID> findCoMemberIdsByUserId(@Param("userId") UUID userId);

    @Query("""
        select m.userId from ServerMember m
        where m.server.id = (
            select c.server.id from Channel c where c.id = :channelId
        )
    """)
    List<UUID> findUserIdsByChannelId(@Param("channelId") Long channelId);
}
