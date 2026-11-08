package com.undercontroll.infrastructure.messaging.producers;

import com.undercontroll.domain.event.AnnouncementCreatedEvent;
import com.undercontroll.domain.event.UserCreatedEvent;
import com.undercontroll.domain.event.OrderBudgetDecidedEvent;
import com.undercontroll.domain.event.OrderStatusChangedEvent;
import com.undercontroll.domain.gateway.NotificationGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
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

    @Value("${spring.rabbitmq.routing-key.order-status-changed:order.status.changed}")
    private String orderStatusChangedRoutingKey;

    @Value("${spring.rabbitmq.routing-key.order-budget-decided:order.budget.decided}")
    private String orderBudgetDecidedRoutingKey;

    @Async
    @Override
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", event.orderId());
        data.put("from", event.from() == null ? null : event.from().name());
        data.put("to", event.to().name());
        data.put("outcome", event.outcome() == null ? null : event.outcome().name());
        data.put("actor", event.actor().name());
        data.put("occurredAt", event.occurredAt().toString());
        publish(orderStatusChangedRoutingKey, "ORDER_STATUS_CHANGED", data, event.orderId());
    }

    @Async
    @Override
    public void handleOrderBudgetDecided(OrderBudgetDecidedEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", event.orderId());
        data.put("decision", event.decision().name());
        data.put("actor", event.actor().name());
        data.put("budgetTotal", event.budgetTotal());
        data.put("occurredAt", event.occurredAt().toString());
        publish(orderBudgetDecidedRoutingKey, "ORDER_BUDGET_DECIDED", data, event.orderId());
    }

    private void publish(String routingKey, String type, Map<String, Object> data, Integer orderId) {
        var payload = Map.of(
                "service", "main-service",
                "type", type,
                "data", data,
                "timestamp", LocalDateTime.now().toString()
        );
        try {
            rabbitTemplate.convertAndSend(notificationExchange, routingKey, payload);
            log.info("Published notification type={} orderId={}", type, orderId);
        } catch (RuntimeException ex) {
            log.error("Failed to publish notification type={} orderId={}", type, orderId, ex);
        }
    }

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
