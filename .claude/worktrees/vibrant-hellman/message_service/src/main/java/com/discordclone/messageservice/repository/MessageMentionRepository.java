package com.discordclone.messageservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.discordclone.messageservice.entity.MessageMention;
import com.discordclone.messageservice.entity.ReactionSourceType;


@Repository
public interface MessageMentionRepository extends JpaRepository<MessageMention, String> {
    List<MessageMention> findBySourceTypeAndSourceId(ReactionSourceType sourceType, String sourceId);
}