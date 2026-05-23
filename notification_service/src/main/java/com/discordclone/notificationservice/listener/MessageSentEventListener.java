package com.discordclone.notificationservice.listener;

import com.discordclone.common.event.MessageSentEvent;
import com.discordclone.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSentEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${app.rabbitmq.message-sent-queue}")
    public void onMessageSent(MessageSentEvent event) {
        log.debug("Received MessageSentEvent: {}", event.getMessageId());
        notificationService.createFromMessageSent(event);
    }
}
