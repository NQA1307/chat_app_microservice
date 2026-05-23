package com.discordclone.messageservice.listener;

import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.discordclone.messageservice.service.PresenceService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketPresenceEventListener {
    private final PresenceService presenceService;


    //Lang nghe event de tao message
    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        UUID userId = extractUserId(event);
        if (userId != null) {
            presenceService.markOnline(userId);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        UUID userId = extractUserId(event);
        if (userId != null) {
            presenceService.markOffline(userId);
        }
    }

    private UUID extractUserId(AbstractSubProtocolEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getFirstNativeHeader("X-User-Id");

        if (userId == null || userId.isBlank()) {
            return null;
        }

        return UUID.fromString(userId);
    }
}
