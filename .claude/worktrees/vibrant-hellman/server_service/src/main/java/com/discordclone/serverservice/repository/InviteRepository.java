package com.discordclone.serverservice.repository;

import com.discordclone.common.enums.InviteStatus;
import com.discordclone.common.enums.InviteType;
import com.discordclone.serverservice.entity.Invite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface InviteRepository extends JpaRepository<Invite, String> {

    List<Invite> findByReceiverIdAndStatusOrderByCreatedAtDesc(
            UUID receiverId,
            InviteStatus status
    );

    Optional<Invite> findByIdAndReceiverId(
            String id,
            UUID receiverId
    );

    boolean existsBySenderIdAndReceiverIdAndTypeAndTargetTypeAndTargetIdAndStatus(
            UUID senderId,
            UUID receiverId,
            InviteType type,
            String targetType,
            String targetId,
            InviteStatus status
    );

    void deleteByTargetTypeAndTargetId(String targetType, String targetId);
}
