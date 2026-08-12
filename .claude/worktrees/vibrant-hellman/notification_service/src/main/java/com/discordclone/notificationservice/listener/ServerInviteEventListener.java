package com.discordclone.notificationservice.listener;

import com.discordclone.common.event.EventEnvelope;
import com.discordclone.common.event.EventTypes;
import com.discordclone.common.event.ServerInviteEvent;
import com.discordclone.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerInviteEventListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${app.rabbitmq.server-invite-sent-queue}")
    public void onServerInviteEvent(EventEnvelope<Object> envelope) {
        if (!EventTypes.SERVER_INVITE_SENT.equals(envelope.eventType())) {
            log.warn("Ignored unexpected event type on server invite queue. eventId={}, eventType={}",
                    envelope.eventId(), envelope.eventType());
            return;
        }

        ServerInviteEvent event = toPayload(envelope.payload(), ServerInviteEvent.class);
        log.debug("Received ServerInviteEvent. eventId={}, inviteId={}", envelope.eventId(), event.inviteId());
        notificationService.createFromServerInvite(event, envelope.eventId());
    }

    private <T> T toPayload(Object payload, Class<T> type) {
        if (type.isInstance(payload)) {
            return type.cast(payload);
        }
        return objectMapper.convertValue(payload, type);
    }
}
