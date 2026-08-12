package com.discordclone.messageservice.repository;

import com.discordclone.messageservice.entity.MessageReaction;
import com.discordclone.messageservice.entity.ReactionSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {

    Optional<MessageReaction> findBySourceTypeAndSourceIdAndUserId(
        ReactionSourceType sourceType,
        String sourceId,
        UUID userId
    );

    List<MessageReaction> findBySourceTypeAndSourceId(
        ReactionSourceType sourceType,
        String sourceId
    );

    @Query("""
            select r.sourceId as sourceId, r.emoji as emoji, count(r.id) as count
            from MessageReaction r
            where r.sourceType = :sourceType
              and r.sourceId in :sourceIds
            group by r.sourceId, r.emoji
            """)
    List<ReactionSummaryRow> summarizeBySourceIds(
        @Param("sourceType") ReactionSourceType sourceType,
        @Param("sourceIds") List<String> sourceIds
    );

    void deleteBySourceTypeAndSourceIdAndUserId(
        ReactionSourceType sourceType,
        String sourceId,
        UUID userId
    );

    interface ReactionSummaryRow {
        String getSourceId();
        String getEmoji();
        long getCount();
    }
}
