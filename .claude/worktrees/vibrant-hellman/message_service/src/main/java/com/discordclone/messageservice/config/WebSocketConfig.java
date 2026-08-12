package com.discordclone.messageservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompChannelInterceptor stompChannelInterceptor;
    private final WebSocketHandshakeLogger webSocketHandshakeLogger;
    private final StompHandshakeHandler stompHandshakeHandler;
    private final String relayHost;
    private final int relayPort;
    private final String relayLogin;
    private final String relayPasscode;

    public WebSocketConfig(
            StompChannelInterceptor stompChannelInterceptor,
            WebSocketHandshakeLogger webSocketHandshakeLogger,
            StompHandshakeHandler stompHandshakeHandler,
            @Value("${rabbitmq.stomp.host:127.0.0.1}") String relayHost,
            @Value("${rabbitmq.stomp.port:61613}") int relayPort,
            @Value("${rabbitmq.stomp.login:guest}") String relayLogin,
            @Value("${rabbitmq.stomp.passcode:guest}") String relayPasscode
    ) {
        this.stompChannelInterceptor = stompChannelInterceptor;
        this.webSocketHandshakeLogger = webSocketHandshakeLogger;
        this.stompHandshakeHandler = stompHandshakeHandler;
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.relayLogin = relayLogin;
        this.relayPasscode = relayPasscode;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableStompBrokerRelay("/topic")
            .setRelayHost(relayHost)
            .setRelayPort(relayPort)
            .setClientLogin(relayLogin)
            .setClientPasscode(relayPasscode)
            .setSystemLogin(relayLogin)
            .setSystemPasscode(relayPasscode);

    registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint kết nối WebSocket, hỗ trợ SockJS fallback
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(stompHandshakeHandler)
                .addInterceptors(webSocketHandshakeLogger);

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(stompHandshakeHandler)
                .addInterceptors(webSocketHandshakeLogger)
                .withSockJS();
    }


    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompChannelInterceptor);
    }
}
