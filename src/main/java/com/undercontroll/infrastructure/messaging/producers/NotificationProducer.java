package com.undercontroll.infrastructure.messaging.producers;

import com.undercontroll.domain.port.out.NotificationPort;
import com.undercontroll.domain.events.AnnouncementCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationProducer implements NotificationPort {

    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.ex.notification}")
    private String notificationExchange;

    @Value("${spring.rabbitmq.routing-key.announcement}")
    private String announcementRoutingKey;


    @Async
    @Override
    public void handleAnnouncementCreated(AnnouncementCreatedEvent event) {
        var data = Map.of(
                "id", event.announcement().getId(),
                "title", event.announcement().getTitle(),
                "content", event.announcement().getContent(),
                "type", event.announcement().getType().name(),
                "publishedAt", event.announcement().getPublishedAt().toString(),
                "token", event.token() != null ? event.token() : ""
        );
        
        var payload = Map.of(
                "service", "main-service",
                "type", "ANNOUNCEMENT_CREATED",
                "data", data,
                "timestamp", java.time.LocalDateTime.now().toString()
        );
        rabbitTemplate.convertAndSend(notificationExchange, announcementRoutingKey, payload);
    }
}

