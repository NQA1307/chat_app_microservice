package com.discordclone.userservice.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.discordclone.common.event.AuthEmailRequestedEvent;
import com.discordclone.common.event.EventTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AuthEmailEventPublisherTest {

    private DomainEventPublisher domainEventPublisher;
    private AuthEmailEventPublisher publisher;

    @BeforeEach
    void setUp() {
        domainEventPublisher = org.mockito.Mockito.mock(DomainEventPublisher.class);
        publisher = new AuthEmailEventPublisher(domainEventPublisher);
        ReflectionTestUtils.setField(publisher, "authEmailRequestedRoutingKey", "auth.email.requested");
        ReflectionTestUtils.setField(publisher, "otpTtlMinutes", 10);
    }

    @Test
    void publishEmailVerificationSendsExpectedEvent() {
        publisher.publishEmailVerification("test@example.com", "123456");

        ArgumentCaptor<AuthEmailRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(AuthEmailRequestedEvent.class);

        verify(domainEventPublisher).publish(
                eq(EventTypes.AUTH_EMAIL_REQUESTED),
                eq("auth.email.requested"),
                eventCaptor.capture()
        );

        AuthEmailRequestedEvent event = eventCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(event.email()).isEqualTo("test@example.com");
        org.assertj.core.api.Assertions.assertThat(event.type()).isEqualTo("EMAIL_VERIFICATION");
        org.assertj.core.api.Assertions.assertThat(event.code()).isEqualTo("123456");
        org.assertj.core.api.Assertions.assertThat(event.expiresInMinutes()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(event.createdAt()).isNotNull();
    }

    @Test
    void publishPasswordResetSendsExpectedEvent() {
        publisher.publishPasswordReset("test@example.com", "654321");

        ArgumentCaptor<AuthEmailRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(AuthEmailRequestedEvent.class);

        verify(domainEventPublisher).publish(
                eq(EventTypes.AUTH_EMAIL_REQUESTED),
                eq("auth.email.requested"),
                eventCaptor.capture()
        );

        AuthEmailRequestedEvent event = eventCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(event.email()).isEqualTo("test@example.com");
        org.assertj.core.api.Assertions.assertThat(event.type()).isEqualTo("PASSWORD_RESET");
        org.assertj.core.api.Assertions.assertThat(event.code()).isEqualTo("654321");
        org.assertj.core.api.Assertions.assertThat(event.expiresInMinutes()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(event.createdAt()).isNotNull();
    }
}
