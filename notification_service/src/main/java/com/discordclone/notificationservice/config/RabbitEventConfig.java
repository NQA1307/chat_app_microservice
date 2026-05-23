package com.discordclone.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
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

    @Bean
    public TopicExchange discordEventsExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue messageSentQueue() {
        return new Queue(messageSentQueueName, true);
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

    @Bean
    public Queue dmSentQueue() {
        return new Queue(dmSentQueueName, true);
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
    public Queue serverInviteSentQueue() {
        return new Queue(serverInviteSentQueueName, true);
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
}
