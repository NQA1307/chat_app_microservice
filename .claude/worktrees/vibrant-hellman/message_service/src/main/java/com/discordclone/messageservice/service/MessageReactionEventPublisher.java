package com.discordclone.messageservice.service;

import com.discordclone.common.event.EventTypes;
import com.discordclone.messageservice.dto.response.MessageReactionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageReactionEventPublisher {

    private final DomainEventPublisher domainEventPublisher;

    @Value("${app.rabbitmq.message-reaction-updated-routing-key:message.reaction.updated}")
    private String routingKey;

    public void publishReactionUpdated(MessageReactionEvent event) {
        domainEventPublisher.publish(
                EventTypes.MESSAGE_REACTION_UPDATED,
                routingKey,
                event
        );
    }

}
