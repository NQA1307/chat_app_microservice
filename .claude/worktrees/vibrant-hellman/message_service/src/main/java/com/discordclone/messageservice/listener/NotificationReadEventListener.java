package com.discordclone.messageservice.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.discordclone.common.event.EventEnvelope;
import com.discordclone.common.event.EventTypes;
import com.discordclone.common.event.NotificationReadEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationReadEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${app.rabbitmq.notification-read-queue}")
    public void handle(EventEnvelope<Object> envelope) {
        if (!EventTypes.NOTIFICATION_READ.equals(envelope.eventType())) {
            log.warn("Ignored unexpected event type on notification read queue. eventId={}, eventType={}",
                    envelope.eventId(), envelope.eventType());
            return;
        }

        NotificationReadEvent event = toPayload(envelope.payload(), NotificationReadEvent.class);
        if (event.getRecipientId() == null) {
            return;
        }

        String destination = "/topic/user." + event.getRecipientId() + ".notifications";
        messagingTemplate.convertAndSend(destination, event);

        log.debug("Broadcast notification read event {} to {}. envelopeEventId={}",
                event.getEventId(), destination, envelope.eventId());
    }

    private <T> T toPayload(Object payload, Class<T> type) {
        if (type.isInstance(payload)) {
            return type.cast(payload);
        }
        return objectMapper.convertValue(payload, type);
    }
}
