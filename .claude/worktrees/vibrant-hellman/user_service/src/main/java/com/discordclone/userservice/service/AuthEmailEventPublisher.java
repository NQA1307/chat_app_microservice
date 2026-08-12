package com.discordclone.userservice.service;

import com.discordclone.common.event.AuthEmailRequestedEvent;
import com.discordclone.common.event.EventTypes;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEmailEventPublisher {

    private static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    private static final String PASSWORD_RESET = "PASSWORD_RESET";

    private final DomainEventPublisher domainEventPublisher;

    @Value("${app.rabbitmq.auth-email-requested-routing-key}")
    private String authEmailRequestedRoutingKey;

    @Value("${app.otp.ttl-minutes:10}")
    private int otpTtlMinutes;

    public void publishEmailVerification(String email, String code) {
        publish(email, EMAIL_VERIFICATION, code);
    }

    public void publishPasswordReset(String email, String code) {
        publish(email, PASSWORD_RESET, code);
    }

    private void publish(String email, String type, String code) {
        log.info("Publishing auth email event. type={}, email={}", type, email);
        domainEventPublisher.publish(
                EventTypes.AUTH_EMAIL_REQUESTED,
                authEmailRequestedRoutingKey,
                new AuthEmailRequestedEvent(email, type, code, otpTtlMinutes, LocalDateTime.now())
        );
    }
}
