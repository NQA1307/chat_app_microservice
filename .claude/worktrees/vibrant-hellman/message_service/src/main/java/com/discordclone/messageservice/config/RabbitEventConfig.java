package com.discordclone.messageservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitEventConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.notification-created-queue}")
    private String notificationCreatedQueue;

    @Value("${app.rabbitmq.notification-created-routing-key}")
    private String notificationCreatedRoutingKey;

    @Value("${app.rabbitmq.notification-read-queue}")
    private String notificationReadQueue;

    @Value("${app.rabbitmq.notification-read-routing-key}")
    private String notificationReadRoutingKey;

    @Bean
    public TopicExchange discordEventsExchange() {
        return new TopicExchange(exchangeName, true, false);
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

    @Bean
    public Queue notificationCreatedQueue() {
        return durableQueueWithDlq(notificationCreatedQueue, notificationCreatedRoutingKey);
    }

    @Bean
    public Binding notificationCreatedBinding(
            TopicExchange discordEventsExchange,
            @Qualifier("notificationCreatedQueue") Queue notificationCreatedQueue
    ) {
        return BindingBuilder
            .bind(notificationCreatedQueue)
            .to(discordEventsExchange)
            .with(notificationCreatedRoutingKey);
    }

    @Bean
    public Queue notificationReadQueue() {
        return durableQueueWithDlq(notificationReadQueue, notificationReadRoutingKey);
    }

    @Bean
    public Binding notificationReadBinding(
            TopicExchange discordEventsExchange,
            @Qualifier("notificationReadQueue") Queue notificationReadQueue
    ) {
        return BindingBuilder
                .bind(notificationReadQueue)
                .to(discordEventsExchange)
                .with(notificationReadRoutingKey);
    }

    @Bean
    public Queue notificationCreatedDlq() {
        return durableDlq(notificationCreatedQueue);
    }

    @Bean
    public Binding notificationCreatedDlqBinding(
            TopicExchange discordEventsExchange,
            @Qualifier("notificationCreatedDlq") Queue notificationCreatedDlq
    ) {
        return BindingBuilder
                .bind(notificationCreatedDlq)
                .to(discordEventsExchange)
                .with(notificationCreatedRoutingKey + ".dlq");
    }

    @Bean
    public Queue notificationReadDlq() {
        return durableDlq(notificationReadQueue);
    }

    @Bean
    public Binding notificationReadDlqBinding(
            TopicExchange discordEventsExchange,
            @Qualifier("notificationReadDlq") Queue notificationReadDlq
    ) {
        return BindingBuilder
                .bind(notificationReadDlq)
                .to(discordEventsExchange)
                .with(notificationReadRoutingKey + ".dlq");
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
