package com.undercontroll.infrastructure.messaging.producers;

import com.undercontroll.infrastructure.events.AnnouncementCreatedEvent;
import com.undercontroll.infrastructure.events.UserCreatedEvent;
import com.undercontroll.domain.gateway.NotificationGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer implements NotificationGateway {

    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.ex.notification}")
    private String notificationExchange;

    @Value("${spring.rabbitmq.routing-key.announcement}")
    private String announcementRoutingKey;

    @Value("${spring.rabbitmq.routing-key.user-created}")
    private String userCreatedRoutingKey;

    @Async
    @Override
    public void handleAnnouncementCreated(AnnouncementCreatedEvent event) {
        Integer announcementId = event.announcement().getId();
        var data = Map.of(
                "id", announcementId,
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
                "timestamp", LocalDateTime.now().toString()
        );
        try {
            rabbitTemplate.convertAndSend(notificationExchange, announcementRoutingKey, payload);
            log.info("Published notification type=ANNOUNCEMENT_CREATED announcementId={}", announcementId);
        } catch (RuntimeException ex) {
            log.error("Failed to publish notification type=ANNOUNCEMENT_CREATED announcementId={}", announcementId, ex);
            throw ex;
        }
    }

    @Async
    @Override
    public void handleUserCreated(UserCreatedEvent event) {
        LocalDateTime createdAt = event.createdAt() != null ? event.createdAt() : LocalDateTime.now();

        var data = Map.of(
                "name", event.name() != null ? event.name() : "",
                "email", event.email() != null ? event.email() : "",
                "createdAt", createdAt.toString()
        );

        var payload = Map.of(
                "service", "main-service",
                "type", "USER_CREATED",
                "data", data,
                "timestamp", LocalDateTime.now().toString()
        );

        try {
            rabbitTemplate.convertAndSend(notificationExchange, userCreatedRoutingKey, payload);
            log.info("Published notification type=USER_CREATED");
        } catch (RuntimeException ex) {
            log.error("Failed to publish notification type=USER_CREATED", ex);
            throw ex;
        }
    }
}
