package com.discordclone.notificationservice.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.discordclone.common.event.DirectMessageSentEvent;
import com.discordclone.common.event.EventEnvelope;
import com.discordclone.common.event.EventTypes;
import com.discordclone.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Component
@RequiredArgsConstructor
@Slf4j
public class DirectMessageSentEventListener {
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${app.rabbitmq.dm-sent-queue}")
    public void onDirectMessageEvent(EventEnvelope<Object> envelope) {
        if (!EventTypes.DM_MESSAGE_SENT.equals(envelope.eventType())) {
            log.warn("Ignored unexpected event type on DM queue. eventId={}, eventType={}",
                    envelope.eventId(), envelope.eventType());
            return;
        }

        DirectMessageSentEvent event = toPayload(envelope.payload(), DirectMessageSentEvent.class);
        log.debug("Received DirectMessageSentEvent. eventId={}, messageId={}", envelope.eventId(), event.messageId());
        notificationService.createFromDirectMessageSent(event, envelope.eventId());
    }

    private <T> T toPayload(Object payload, Class<T> type) {
        if (type.isInstance(payload)) {
            return type.cast(payload);
        }
        return objectMapper.convertValue(payload, type);
    }
}
