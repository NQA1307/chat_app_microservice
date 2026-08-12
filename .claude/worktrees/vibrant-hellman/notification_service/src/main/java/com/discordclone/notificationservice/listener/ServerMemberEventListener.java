package com.discordclone.notificationservice.listener;

import com.discordclone.common.event.EventEnvelope;
import com.discordclone.common.event.EventTypes;
import com.discordclone.common.event.ServerMemberEvent;
import com.discordclone.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerMemberEventListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${app.rabbitmq.server-member-queue}")
    public void onServerMemberEvent(EventEnvelope<Object> envelope) {
        String eventType = envelope.eventType();
        if (!EventTypes.SERVER_MEMBER_KICKED.equals(eventType) && 
            !EventTypes.SERVER_MEMBER_LEFT.equals(eventType)) {
            return;
        }

        ServerMemberEvent event = toPayload(envelope.payload(), ServerMemberEvent.class);
        log.debug("Received ServerMemberEvent. type={}, action={}, target={}", 
                eventType, event.action(), event.targetId());
        
        notificationService.createFromServerMemberAction(event, envelope.eventId());
    }

    private <T> T toPayload(Object payload, Class<T> type) {
        if (type.isInstance(payload)) {
            return type.cast(payload);
        }
        return objectMapper.convertValue(payload, type);
    }
}
