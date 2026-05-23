package com.discordclone.notificationservice.listener;

import com.discordclone.common.event.ServerInviteEvent;
import com.discordclone.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerInviteEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${app.rabbitmq.server-invite-sent-queue}")
    public void onServerInviteEvent(ServerInviteEvent event) {
        log.debug("Received ServerInviteEvent: {}", event.inviteId());
        notificationService.createFromServerInvite(event);
    }
}
