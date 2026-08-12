package com.discordclone.messageservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.discordclone.messageservice.entity.ChannelReadState;

public interface ChannelReadStateRepository extends JpaRepository<ChannelReadState, Long> {

    Optional<ChannelReadState> findByChannelIdAndUserId(Long channelId, UUID userId);
    
   List<ChannelReadState> findByUserId(UUID userId); 
}
