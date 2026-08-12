package com.discordclone.messageservice.listener;

import com.discordclone.common.event.EventEnvelope;
import com.discordclone.common.event.EventTypes;
import com.discordclone.common.event.NotificationCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCreatedEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${app.rabbitmq.notification-created-queue}")
    public void handle(EventEnvelope<Object> envelope) {
        if (!EventTypes.NOTIFICATION_CREATED.equals(envelope.eventType())) {
            log.warn("Ignored unexpected event type on notification created queue. eventId={}, eventType={}",
                    envelope.eventId(), envelope.eventType());
            return;
        }

        NotificationCreatedEvent event = toPayload(envelope.payload(), NotificationCreatedEvent.class);
        if (event.getRecipientId() == null) {
            return;
        }

        String destination = "/topic/user." + event.getRecipientId() + ".notifications";
        messagingTemplate.convertAndSend(destination, event);

        log.debug("Broadcast notification {} to {}. eventId={}", event.getId(), destination, envelope.eventId());
    }

    private <T> T toPayload(Object payload, Class<T> type) {
        if (type.isInstance(payload)) {
            return type.cast(payload);
        }
        return objectMapper.convertValue(payload, type);
    }
}
