package com.discordclone.messageservice.listener;

import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.discordclone.messageservice.service.PresenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPresenceEventListener {
    private final PresenceService presenceService;


    //Lang nghe event de tao message
    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        UUID userId = extractUserId(accessor);
        String sessionId = accessor.getSessionId();

        if (userId != null && sessionId != null) {
            log.debug("Marking user {} online for websocket session {}", userId, sessionId);
            presenceService.markOnline(userId, sessionId);
        } else {
            log.debug("Skipping websocket connect presence update. userId={}, sessionId={}", userId, sessionId);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        UUID userId = extractUserId(accessor);
        String sessionId = accessor.getSessionId();

        if (userId != null && sessionId != null) {
            log.debug("Marking user {} offline for websocket session {}", userId, sessionId);
            presenceService.markOffline(userId, sessionId);
        } else {
            log.debug("Skipping websocket disconnect presence update. userId={}, sessionId={}", userId, sessionId);
        }
    }

    
    private UUID extractUserId(StompHeaderAccessor accessor) {
        if (accessor.getSessionAttributes() == null) {
            return null;
        }

        Object userId = accessor.getSessionAttributes().get("userId");
        if (userId instanceof UUID uuid) {
            return uuid;
        }

        String nativeUserId = accessor.getFirstNativeHeader("X-User-Id");
        if (nativeUserId != null && !nativeUserId.isBlank()) {
            return UUID.fromString(nativeUserId);
        }

        return null;
    }
}
