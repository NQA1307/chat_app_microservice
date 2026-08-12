package com.discordclone.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitEventConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.message-sent-queue}")
    private String messageSentQueueName;

    @Value("${app.rabbitmq.message-sent-routing-key}")
    private String messageSentRoutingKey;

    @Value("${app.rabbitmq.dm-sent-queue}")
    private String dmSentQueueName;

    @Value("${app.rabbitmq.dm-sent-routing-key}")
    private String dmSentRoutingKey;

    @Value("${app.rabbitmq.server-invite-sent-queue}")
    private String serverInviteSentQueueName;

    @Value("${app.rabbitmq.server-invite-sent-routing-key}")
    private String serverInviteSentRoutingKey;

    @Value("${app.rabbitmq.auth-email-requested-queue}")
    private String authEmailRequestedQueueName;

    @Value("${app.rabbitmq.auth-email-requested-routing-key}")
    private String authEmailRequestedRoutingKey;

    @Value("${app.rabbitmq.friend-request-queue}")
    private String friendRequestQueueName;

    @Value("${app.rabbitmq.friend-request-routing-key}")
    private String friendRequestRoutingKey;

    @Value("${app.rabbitmq.server-member-queue}")
    private String serverMemberQueueName;

    @Value("${app.rabbitmq.server-member-routing-key}")
    private String serverMemberRoutingKey;

    @Value("${app.rabbitmq.user-blocked-queue}")
    private String userBlockedQueueName;

    @Value("${app.rabbitmq.user-blocked-routing-key}")
    private String userBlockedRoutingKey;

    @Bean
    public TopicExchange discordEventsExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue messageSentQueue() {
        return durableQueueWithDlq(messageSentQueueName, messageSentRoutingKey);
    }

    @Bean
    public Binding messageSentBinding(
            @Qualifier("messageSentQueue") Queue messageSentQueue,
            TopicExchange discordEventsExchange
    ) {
        return BindingBuilder
                .bind(messageSentQueue)
                .to(discordEventsExchange)
                .with(messageSentRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }

    //Tao queue gui dm
    @Bean
    public Queue dmSentQueue() {
        return durableQueueWithDlq(dmSentQueueName, dmSentRoutingKey);
    }


    //Queue khi dm message dead
    @Bean
    public Queue dmSentDlq() {
        return durableDlq(dmSentQueueName);
    }

    @Bean 
    public Binding dmSentBinding(
            @Qualifier("dmSentQueue") Queue dmSentQueue,
            TopicExchange discordEventsExchange
    ) {
        return BindingBuilder
                .bind(dmSentQueue)
                .to(discordEventsExchange)
                .with(dmSentRoutingKey);
    }
    
    @Bean
    public Binding dmSentDlqBinding(
            @Qualifier("dmSentDlq") Queue dmSentDlq,
            TopicExchange discordEventsExchange
    ) {
        return BindingBuilder
                .bind(dmSentDlq)
                .to(discordEventsExchange)
                .with(dmSentRoutingKey + ".dlq");
    }

    @Bean
    public Queue messageSentDlq() {
        return durableDlq(messageSentQueueName);
    }

    @Bean
    public Binding messageSentDlqBinding(
            @Qualifier("messageSentDlq") Queue messageSentDlq,
            TopicExchange discordEventsExchange
    ) {
        return BindingBuilder
                .bind(messageSentDlq)
                .to(discordEventsExchange)
                .with(messageSentRoutingKey + ".dlq");
    }
    

    @Bean
    public Queue serverInviteSentQueue() {
        return durableQueueWithDlq(serverInviteSentQueueName, serverInviteSentRoutingKey);
    }

    @Bean
    public Binding serverInviteSentBinding(
            @Qualifier("serverInviteSentQueue") Queue serverInviteSentQueue,
            TopicExchange discordEventsExchange
    ) {
        return BindingBuilder
                .bind(serverInviteSentQueue)
                .to(discordEventsExchange)
                .with(serverInviteSentRoutingKey);
    }

    @Bean
    public Queue serverInviteSentDlq() {
        return durableDlq(serverInviteSentQueueName);
    }

    @Bean
    public Binding serverInviteSentDlqBinding(
            @Qualifier("serverInviteSentDlq") Queue serverInviteSentDlq,
            TopicExchange discordEventsExchange
    ) {
        return BindingBuilder
                .bind(serverInviteSentDlq)
                .to(discordEventsExchange)
                .with(serverInviteSentRoutingKey + ".dlq");
    }

    @Bean
    public Queue authEmailRequestedQueue() {
        return durableQueueWithDlq(authEmailRequestedQueueName, authEmailRequestedRoutingKey);
    }

    @Bean
    public Binding authEmailRequestedBinding(
            @Qualifier("authEmailRequestedQueue") Queue authEmailRequestedQueue,
            TopicExchange discordEventsExchange
    ) {
        return BindingBuilder
                .bind(authEmailRequestedQueue)
                .to(discordEventsExchange)
                .with(authEmailRequestedRoutingKey);
    }

    @Bean
    public Queue authEmailRequestedDlq() {
        return durableDlq(authEmailRequestedQueueName);
    }

    @Bean
    public Binding authEmailRequestedDlqBinding(
            @Qualifier("authEmailRequestedDlq") Queue authEmailRequestedDlq,
            TopicExchange discordEventsExchange
    ) {
        return BindingBuilder
                .bind(authEmailRequestedDlq)
                .to(discordEventsExchange)
                .with(authEmailRequestedRoutingKey + ".dlq");
    }

    @Bean
    public Queue friendRequestQueue() {
        return durableQueueWithDlq(friendRequestQueueName, "friend.request.sent");
    }

    @Bean
    public Binding friendRequestBinding(
            @Qualifier("friendRequestQueue") Queue friendRequestQueue,
            TopicExchange discordEventsExchange
    ) {
        return BindingBuilder
                .bind(friendRequestQueue)
                .to(discordEventsExchange)
                .with(friendRequestRoutingKey);
    }

    @Bean
    public Queue friendRequestDlq() {
        return durableDlq(friendRequestQueueName);
    }

    @Bean
    public Binding friendRequestDlqBinding(
            @Qualifier("friendRequestDlq") Queue friendRequestDlq,
            TopicExchange discordEventsExchange
    ) {
        return BindingBuilder
                .bind(friendRequestDlq)
                .to(discordEventsExchange)
                .with("friend.request.sent.dlq");
    }

    @Bean
    public Queue serverMemberQueue() {
        return durableQueueWithDlq(serverMemberQueueName, "server.member.action");
    }

    @Bean
    public Binding serverMemberBinding(
            @Qualifier("serverMemberQueue") Queue serverMemberQueue,
            TopicExchange discordEventsExchange
    ) {
        return BindingBuilder
                .bind(serverMemberQueue)
                .to(discordEventsExchange)
                .with(serverMemberRoutingKey);
    }

    @Bean
    public Queue serverMemberDlq() {
        return durableDlq(serverMemberQueueName);
    }

    @Bean
    public Binding serverMemberDlqBinding(
            @Qualifier("serverMemberDlq") Queue serverMemberDlq,
            TopicExchange discordEventsExchange
    ) {
        return BindingBuilder
                .bind(serverMemberDlq)
                .to(discordEventsExchange)
                .with("server.member.action.dlq");
    }

    @Bean
    public Queue userBlockedQueue() {
        return durableQueueWithDlq(userBlockedQueueName, "user.blocked.action");
    }

    @Bean
    public Binding userBlockedBinding(
            @Qualifier("userBlockedQueue") Queue userBlockedQueue,
            TopicExchange discordEventsExchange
    ) {
        return BindingBuilder
                .bind(userBlockedQueue)
                .to(discordEventsExchange)
                .with(userBlockedRoutingKey);
    }

    @Bean
    public Queue userBlockedDlq() {
        return durableDlq(userBlockedQueueName);
    }

    @Bean
    public Binding userBlockedDlqBinding(
            @Qualifier("userBlockedDlq") Queue userBlockedDlq,
            TopicExchange discordEventsExchange
    ) {
        return BindingBuilder
                .bind(userBlockedDlq)
                .to(discordEventsExchange)
                .with("user.blocked.action.dlq");
    }

    private Queue durableQueueWithDlq(String queueName, String routingKey) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", exchangeName)
                .withArgument("x-dead-letter-routing-key", routingKey + ".dlq")
                .build();
    }

    private Queue durableDlq(String queueName) {
        return QueueBuilder.durable(queueName + ".dlq").build();
    }
}
