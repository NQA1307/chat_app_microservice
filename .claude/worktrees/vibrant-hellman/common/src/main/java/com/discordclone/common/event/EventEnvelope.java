package com.discordclone.common.event;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    String producer,
    T payload
) {

    public static <T> EventEnvelope<T> of(String eventType, String producer, T payload) {
        return new EventEnvelope<T>(
            UUID.randomUUID(),
            eventType,
            Instant.now(),
            producer,
            payload
        );
    }
}
