package com.discordclone.messageservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class StompHandshakeHandler extends DefaultHandshakeHandler {

    private static final Set<String> SUPPORTED_PROTOCOLS = Set.of("v12.stomp", "v11.stomp", "v10.stomp");

    @Override
    protected String selectProtocol(List<String> requestedProtocols, WebSocketHandler webSocketHandler) {
        for (String requestedProtocol : requestedProtocols) {
            for (String protocol : requestedProtocol.split(",")) {
                String normalized = protocol.trim();
                if (SUPPORTED_PROTOCOLS.contains(normalized)) {
                    log.info("Selected WebSocket STOMP subprotocol={}", normalized);
                    return normalized;
                }
            }
        }

        return super.selectProtocol(requestedProtocols, webSocketHandler);
    }
}
