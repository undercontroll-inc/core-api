package com.undercontroll.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {


    public static final String NOTIFICATION_EXCHANGE = "notification.events";
    public static final String RETRY_EXCHANGE = "notification.retry";
    public static final String DLQ_EXCHANGE = "notification.dlq";

    public static final String EMAIL_QUEUE = "notification.email.queue";
    public static final String EMAIL_RETRY_QUEUE = "notification.email.retry.queue";
    public static final String EMAIL_DLQ = "notification.email.dlq";


    public static final String EMAIL_SEND_ROUTING = "email.send";
    public static final String EMAIL_RETRY_ROUTING = "email.retry";
    public static final String EMAIL_DLQ_ROUTING = "email.dlq";

    public static final String ANNOUNCEMENT_EVENTS = "announcement.*";


    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public TopicExchange retryExchange() {
        return new TopicExchange(RETRY_EXCHANGE);
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(DLQ_EXCHANGE);
    }


    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", RETRY_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", EMAIL_RETRY_ROUTING)
                .build();
    }


    @Bean
    public Queue emailRetryQueue() {
        return QueueBuilder.durable(EMAIL_RETRY_QUEUE)
                .withArgument("x-message-ttl", 10000) // 10s
                .withArgument("x-dead-letter-exchange", NOTIFICATION_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", EMAIL_SEND_ROUTING)
                .build();
    }

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange notificationExchange) {
        return BindingBuilder
                .bind(emailQueue)
                .to(notificationExchange)
                .with(ANNOUNCEMENT_EVENTS);
    }

    @Bean
    public Binding retryBinding(Queue emailRetryQueue, TopicExchange retryExchange) {
        return BindingBuilder
                .bind(emailRetryQueue)
                .to(retryExchange)
                .with(EMAIL_RETRY_ROUTING);
    }

    @Bean
    public Binding dlqBinding(Queue emailDlq, TopicExchange dlqExchange) {
        return BindingBuilder
                .bind(emailDlq)
                .to(dlqExchange)
                .with(EMAIL_DLQ_ROUTING);
    }
}
