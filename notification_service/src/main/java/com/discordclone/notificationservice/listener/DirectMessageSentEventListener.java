package com.discordclone.notificationservice.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.discordclone.common.event.DirectMessageSentEvent;
import com.discordclone.notificationservice.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Component
@RequiredArgsConstructor
@Slf4j
public class DirectMessageSentEventListener {
    private final NotificationService notificationService;

    @RabbitListener(queues = "${app.rabbitmq.dm-sent-queue}")
    public void onDirectMessageEvent(DirectMessageSentEvent event) {
        log.debug("Received DirectMessageSentEvent: {}", event.messageId());
        notificationService.createFromDirectMessageSent(event);
    }

}
