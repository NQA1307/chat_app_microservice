package com.discordclone.notificationservice.service;

import com.discordclone.common.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainEventPublisher {

    private static final String PRODUCER = "notification-service";

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    public <T> void publish(String eventType, String routingKey, T payload) {
        EventEnvelope<T> envelope = EventEnvelope.of(eventType, PRODUCER, payload);

        rabbitTemplate.convertAndSend(exchange, routingKey, envelope);

        log.info(
                "Published domain event. eventId={}, eventType={}, routingKey={}",
                envelope.eventId(),
                envelope.eventType(),
                routingKey
        );
    }
}