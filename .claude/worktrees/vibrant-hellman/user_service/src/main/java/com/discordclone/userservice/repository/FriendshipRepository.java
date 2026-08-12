package com.discordclone.userservice.repository;

import com.discordclone.userservice.entity.Friendship;
import com.discordclone.userservice.entity.Friendship.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    @Query("""
            select f from Friendship f
            where (f.requesterId = :userA and f.receiverId = :userB)
               or (f.requesterId = :userB and f.receiverId = :userA)
            """)
    Optional<Friendship> findBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);

    List<Friendship> findByReceiverIdAndStatus(UUID receiverId, FriendshipStatus status);

    List<Friendship> findByRequesterIdAndStatus(UUID requesterId, FriendshipStatus status);

    @Query("""
            select f from Friendship f
            where (f.requesterId = :userId or f.receiverId = :userId)
              and f.status = :status
            order by f.updatedAt desc
            """)
    List<Friendship> findByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") FriendshipStatus status
    );
}
