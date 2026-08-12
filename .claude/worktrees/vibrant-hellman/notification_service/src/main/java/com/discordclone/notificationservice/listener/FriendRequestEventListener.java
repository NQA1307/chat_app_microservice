package com.discordclone.notificationservice.listener;

import com.discordclone.common.event.EventEnvelope;
import com.discordclone.common.event.EventTypes;
import com.discordclone.common.event.FriendRequestEvent;
import com.discordclone.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendRequestEventListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${app.rabbitmq.friend-request-queue}")
    public void onFriendRequestEvent(EventEnvelope<Object> envelope) {
        String eventType = envelope.eventType();
        if (!EventTypes.FRIEND_REQUEST_SENT.equals(eventType) && 
            !EventTypes.FRIEND_REQUEST_ACCEPTED.equals(eventType)) {
            log.warn("Ignored unexpected event type on friend request queue. eventId={}, eventType={}",
                    envelope.eventId(), eventType);
            return;
        }

        FriendRequestEvent event = toPayload(envelope.payload(), FriendRequestEvent.class);
        log.debug("Received FriendRequestEvent. eventId={}, type={}, sender={}", 
                envelope.eventId(), event.type(), event.senderUsername());
        
        notificationService.createFromFriendRequest(event, envelope.eventId());
    }

    private <T> T toPayload(Object payload, Class<T> type) {
        if (type.isInstance(payload)) {
            return type.cast(payload);
        }
        return objectMapper.convertValue(payload, type);
    }
}
