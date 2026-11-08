package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.event.OrderBudgetDecidedEvent;
import com.undercontroll.domain.event.OrderStatusChangedEvent;
import com.undercontroll.domain.gateway.MetricName;
import com.undercontroll.domain.gateway.MetricsGateway;
import com.undercontroll.domain.gateway.OrderEventPublisher;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.OrderStatusEventGateway;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderStatusEvent;
import com.undercontroll.domain.order.OrderLifecycle;
import com.undercontroll.domain.order.TrackingTokens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLifecycleService {

    private final OrderGateway orderGateway;
    private final OrderStatusEventGateway statusEvents;
    private final OrderEventPublisher eventPublisher;
    private final MetricsGateway metricsGateway;
    private final Clock clock;

    @Transactional
    @CacheEvict(value = {"orders", "ordersByUser", "order", "dashboardMetrics"}, allEntries = true)
    public Order transition(Integer orderId, OrderStatus to, OrderOutcome outcome, OrderActor actor, String note) {
        Order order = load(orderId);
        Instant now = clock.instant();
        if (to == OrderStatus.AWAITING_APPROVAL) {
            ensureToken(order);
        }
        OrderStatusEvent event = OrderLifecycle.transition(order, to, outcome, actor, note, now);
        return persist(order, event, now);
    }

    @Transactional
    @CacheEvict(value = {"orders", "ordersByUser", "order", "dashboardMetrics"}, allEntries = true)
    public Order decide(Integer orderId, ApprovalDecision decision, OrderActor actor, String note) {
        Order order = load(orderId);
        Instant now = clock.instant();
        Optional<OrderStatusEvent> event = OrderLifecycle.decideBudget(order, decision, actor, note, now);
        if (event.isEmpty()) {
            return order;
        }
        return persist(order, event.get(), now);
    }

    @Transactional
    public Order ensureTrackingToken(Integer orderId) {
        Order order = load(orderId);
        if (order.getTrackingToken() != null) {
            return order;
        }
        ensureToken(order);
        return orderGateway.save(order);
    }

    private Order persist(Order order, OrderStatusEvent event, Instant now) {
        Order saved = orderGateway.save(order);
        statusEvents.save(event);
        record(event);
        eventPublisher.statusChanged(new OrderStatusChangedEvent(
                saved.getId(), event.from(), event.to(), event.outcome(), event.actor(), now));
        if (event.from() == OrderStatus.AWAITING_APPROVAL && event.to() != OrderStatus.RECEIVED) {
            eventPublisher.budgetDecided(new OrderBudgetDecidedEvent(
                    saved.getId(), saved.getApprovalDecision(), event.actor(), saved.getBudgetTotal(), now));
        }
        log.info("Order {} moved {} -> {} outcome={} actor={}", saved.getId(), event.from(), event.to(), event.outcome(), event.actor());
        return saved;
    }

    private void record(OrderStatusEvent event) {
        if (event.to() == OrderStatus.READY_FOR_PICKUP && event.outcome() == OrderOutcome.REPAIRED) {
            metricsGateway.increment(MetricName.ORDER_COMPLETED);
        }
        if (event.from() == OrderStatus.AWAITING_APPROVAL && event.to() == OrderStatus.IN_REPAIR) {
            metricsGateway.increment(MetricName.ORDER_BUDGET_APPROVED);
        }
        if (event.from() == OrderStatus.AWAITING_APPROVAL && event.to() == OrderStatus.READY_FOR_PICKUP) {
            metricsGateway.increment(MetricName.ORDER_BUDGET_DECLINED);
        }
    }

    private void ensureToken(Order order) {
        if (order.getTrackingToken() == null) {
            order.setTrackingToken(TrackingTokens.generate());
        }
    }

    private Order load(Integer orderId) {
        return orderGateway.findById(orderId).orElseThrow(() -> OrderAccess.notFound(orderId));
    }
}
