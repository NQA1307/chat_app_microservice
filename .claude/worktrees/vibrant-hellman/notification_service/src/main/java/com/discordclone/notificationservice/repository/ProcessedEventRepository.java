package com.discordclone.notificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.discordclone.notificationservice.entity.ProcessedEvent;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String>{

    @Modifying
    @Query(value = """
            insert into processed_events (event_id, event_type)
            values (:eventId, :eventType)
            on conflict (event_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(String eventId, String eventType);
}
