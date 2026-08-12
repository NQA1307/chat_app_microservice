package com.discordclone.notificationservice.listener;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.discordclone.common.event.AuthEmailRequestedEvent;
import com.discordclone.common.event.EventEnvelope;
import com.discordclone.common.event.EventTypes;
import com.discordclone.notificationservice.service.EmailSenderService;
import com.discordclone.notificationservice.service.ProcessedEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthEmailRequestedEventListenerTest {

    private EmailSenderService emailSenderService;
    private ProcessedEventService processedEventService;
    private AuthEmailRequestedEventListener listener;

    @BeforeEach
    void setUp() {
        emailSenderService = org.mockito.Mockito.mock(EmailSenderService.class);
        processedEventService = org.mockito.Mockito.mock(ProcessedEventService.class);
        listener = new AuthEmailRequestedEventListener(
                emailSenderService,
                processedEventService,
                new ObjectMapper()
        );
    }

    @Test
    void onAuthEmailRequestedSendsVerificationEmail() {
        AuthEmailRequestedEvent event = new AuthEmailRequestedEvent(
                "test@example.com",
                "EMAIL_VERIFICATION",
                "123456",
                10,
                LocalDateTime.now()
        );

        EventEnvelope<Object> envelope = EventEnvelope.of(
                EventTypes.AUTH_EMAIL_REQUESTED,
                "user-service",
                event
        );

        listener.onAuthEmailRequested(envelope);

        verify(emailSenderService).sendEmailVerificationCode("test@example.com", "123456", 10);
        verify(processedEventService).markProcessed(envelope.eventId(), EventTypes.AUTH_EMAIL_REQUESTED);
        verify(emailSenderService, never()).sendPasswordResetCode(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void onAuthEmailRequestedSendsPasswordResetEmail() {
        AuthEmailRequestedEvent event = new AuthEmailRequestedEvent(
                "test@example.com",
                "PASSWORD_RESET",
                "654321",
                10,
                LocalDateTime.now()
        );

        EventEnvelope<Object> envelope = EventEnvelope.of(
                EventTypes.AUTH_EMAIL_REQUESTED,
                "user-service",
                event
        );

        listener.onAuthEmailRequested(envelope);

        verify(emailSenderService).sendPasswordResetCode("test@example.com", "654321", 10);
        verify(processedEventService).markProcessed(envelope.eventId(), EventTypes.AUTH_EMAIL_REQUESTED);
        verify(emailSenderService, never()).sendEmailVerificationCode(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void onAuthEmailRequestedSkipsDuplicateEvent() {
        AuthEmailRequestedEvent event = new AuthEmailRequestedEvent(
                "test@example.com",
                "EMAIL_VERIFICATION",
                "123456",
                10,
                LocalDateTime.now()
        );
        EventEnvelope<Object> envelope = EventEnvelope.of(
                EventTypes.AUTH_EMAIL_REQUESTED,
                "user-service",
                event
        );

        when(processedEventService.isProcessed(envelope.eventId())).thenReturn(true);

        listener.onAuthEmailRequested(envelope);

        verify(emailSenderService, never()).sendEmailVerificationCode(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt()
        );
        verify(processedEventService, never()).markProcessed(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }
}
