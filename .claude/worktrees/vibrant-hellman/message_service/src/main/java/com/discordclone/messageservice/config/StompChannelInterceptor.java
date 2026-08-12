package com.discordclone.messageservice.config;

import com.discordclone.messageservice.security.WebSocketJwtService;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StompChannelInterceptor implements ChannelInterceptor {

    private final WebSocketJwtService webSocketJwtService;

    public StompChannelInterceptor(WebSocketJwtService webSocketJwtService) {
        this.webSocketJwtService = webSocketJwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = resolveAuthorization(accessor);
            log.debug("WebSocket STOMP CONNECT received. hasAuthorization={}", authorization != null);

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null) {
                sessionAttributes = new HashMap<>();
                accessor.setSessionAttributes(sessionAttributes);
            }
            try {
                WebSocketJwtService.AuthenticatedUser user = webSocketJwtService.parseAccessToken(authorization);
                sessionAttributes.put("userId", user.userId());
                sessionAttributes.put("username", user.username());
                log.info("WebSocket STOMP authenticated. userId={}", user.userId());
            } catch (RuntimeException e) {
                log.warn("WebSocket STOMP authorization failed: {}", e.getMessage());
                throw new MessagingException("Invalid WebSocket authorization", e);
            }
        }

        return message;
    }

    private String resolveAuthorization(StompHeaderAccessor accessor) {
        String authorization = firstNativeHeader(accessor, "Authorization", "authorization");
        if (authorization != null) {
            return authorization;
        }

        String token = firstNativeHeader(accessor, "access_token", "access-token", "token");
        if (token != null) {
            return toAuthorizationHeader(token);
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return null;
        }

        Object handshakeAuthorization = sessionAttributes.get("authorization");
        if (handshakeAuthorization instanceof String value && !value.isBlank()) {
            return value;
        }

        return null;
    }

    private String firstNativeHeader(StompHeaderAccessor accessor, String... names) {
        for (String name : names) {
            String value = accessor.getFirstNativeHeader(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String toAuthorizationHeader(String token) {
        return token.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())
                ? token
                : "Bearer " + token;
    }
}
