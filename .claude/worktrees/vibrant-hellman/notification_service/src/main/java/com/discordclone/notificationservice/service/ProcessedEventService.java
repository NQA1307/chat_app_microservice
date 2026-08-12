package com.discordclone.notificationservice.service;

import com.discordclone.notificationservice.entity.ProcessedEvent;
import com.discordclone.notificationservice.repository.ProcessedEventRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    public boolean isProcessed(UUID eventId) {
        return processedEventRepository.existsById(eventId.toString());
    }

    public void markProcessed(UUID eventId, String eventType) {
        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(eventId.toString())
                        .eventType(eventType)
                        .build()
        );
    }

    public boolean tryMarkProcessing(UUID eventId, String eventType) {
        return processedEventRepository.insertIfAbsent(eventId.toString(), eventType) == 1;
    }
}
