package com.discordclone.messageservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class WebSocketHandshakeLogger implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        log.info(
                "WebSocket handshake. path={}, protocol={}, origin={}",
                request.getURI().getPath(),
                request.getHeaders().getFirst("Sec-WebSocket-Protocol"),
                request.getHeaders().getFirst("Origin")
        );

        String authorization = request.getHeaders().getFirst("Authorization");
        if (authorization != null && !authorization.isBlank()) {
            attributes.put("authorization", authorization);
        }

        String queryToken = extractTokenFromQuery(request.getURI().getRawQuery());
        if (queryToken != null) {
            attributes.put("authorization", toAuthorizationHeader(queryToken));
        }

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private String extractTokenFromQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }

        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length != 2) {
                continue;
            }

            String key = decode(parts[0]);
            if (!"access_token".equals(key) && !"token".equals(key)) {
                continue;
            }

            String value = decode(parts[1]);
            return value.isBlank() ? null : value;
        }

        return null;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String toAuthorizationHeader(String token) {
        return token.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())
                ? token
                : "Bearer " + token;
    }
}
