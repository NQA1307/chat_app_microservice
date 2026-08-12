package com.discordclone.notificationservice.listener;

import com.discordclone.common.event.EventEnvelope;
import com.discordclone.common.event.EventTypes;
import com.discordclone.common.event.UserBlockEvent;
import com.discordclone.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserBlockEventListener {

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${app.rabbitmq.user-blocked-queue:notification.user-blocked.queue}")
    public void onUserBlockEvent(EventEnvelope<Object> envelope) {
        if (!EventTypes.USER_BLOCKED.equals(envelope.eventType())) {
            return;
        }

        UserBlockEvent event = toPayload(envelope.payload(), UserBlockEvent.class);
        log.info("User {} {} user {}", 
                event.blockerId(), 
                event.blocked() ? "blocked" : "unblocked", 
                event.blockedId());
        
        // Note: We don't usually send a notification to the blocked user.
        // This listener can be used for future logic like clearing cache or 
        // terminating active calls between these users.
    }

    private <T> T toPayload(Object payload, Class<T> type) {
        if (type.isInstance(payload)) {
            return type.cast(payload);
        }
        return objectMapper.convertValue(payload, type);
    }
}
