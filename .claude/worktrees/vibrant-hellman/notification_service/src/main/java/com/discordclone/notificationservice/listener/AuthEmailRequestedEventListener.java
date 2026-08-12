package com.discordclone.notificationservice.listener;

import com.discordclone.common.event.AuthEmailRequestedEvent;
import com.discordclone.common.event.EventEnvelope;
import com.discordclone.common.event.EventTypes;
import com.discordclone.notificationservice.service.EmailSenderService;
import com.discordclone.notificationservice.service.ProcessedEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEmailRequestedEventListener {

    private static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    private static final String PASSWORD_RESET = "PASSWORD_RESET";

    private final EmailSenderService emailSenderService;
    private final ProcessedEventService processedEventService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${app.rabbitmq.auth-email-requested-queue}")
    public void onAuthEmailRequested(EventEnvelope<Object> envelope) {
        if (!EventTypes.AUTH_EMAIL_REQUESTED.equals(envelope.eventType())) {
            log.warn("Ignored unexpected event type on auth email queue. eventId={}, eventType={}",
                    envelope.eventId(), envelope.eventType());
            return;
        }

        if (processedEventService.isProcessed(envelope.eventId())) {
            log.debug("Skipped duplicate auth email event. eventId={}", envelope.eventId());
            return;
        }

        AuthEmailRequestedEvent event = toPayload(envelope.payload(), AuthEmailRequestedEvent.class);
        log.info("Received auth email event. eventId={}, type={}, email={}",
                envelope.eventId(), event.type(), event.email());

        if (EMAIL_VERIFICATION.equals(event.type())) {
            emailSenderService.sendEmailVerificationCode(event.email(), event.code(), event.expiresInMinutes());
            processedEventService.markProcessed(envelope.eventId(), envelope.eventType());
            return;
        }

        if (PASSWORD_RESET.equals(event.type())) {
            emailSenderService.sendPasswordResetCode(event.email(), event.code(), event.expiresInMinutes());
            processedEventService.markProcessed(envelope.eventId(), envelope.eventType());
            return;
        }

        log.warn("Unknown auth email event type: {}", event.type());
    }

    private <T> T toPayload(Object payload, Class<T> type) {
        if (type.isInstance(payload)) {
            return type.cast(payload);
        }
        return objectMapper.convertValue(payload, type);
    }
}
