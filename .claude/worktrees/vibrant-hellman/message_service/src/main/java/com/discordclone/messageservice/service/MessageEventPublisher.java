package com.discordclone.messageservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.discordclone.common.event.DirectMessageSentEvent;
import com.discordclone.common.event.EventTypes;
import com.discordclone.common.event.MessageSentEvent;
import com.discordclone.messageservice.dto.response.MessageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageEventPublisher {

    private final DomainEventPublisher domainEventPublisher;

    @Value("${app.rabbitmq.message-sent-routing-key}")
    private String messageSentRoutingKey;

    @Value("${app.rabbitmq.dm-sent-routing-key}")
    private String dmSentRoutingKey;

    public void publishMessageSent(MessageResponse message) {
        MessageSentEvent event = MessageSentEvent.builder()
                .messageId(message.getId())
                .channelId(message.getChannelId())
                .senderId(message.getSenderId())
                .senderUsername(message.getSenderUsername())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();

        publishMessageSent(event);
    }

    public void publishMessageSent(MessageSentEvent event) {
        domainEventPublisher.publish(
                EventTypes.MESSAGE_SENT,
                messageSentRoutingKey,
                event
        );
    }

    public void publishDirectMessageSent(DirectMessageSentEvent event) {
        domainEventPublisher.publish(
                EventTypes.DM_MESSAGE_SENT,
                dmSentRoutingKey,
                event
        );
    }
}

