package com.undercontroll.infrastructure.messaging;

import com.undercontroll.domain.event.OrderBudgetDecidedEvent;
import com.undercontroll.domain.event.OrderStatusChangedEvent;
import com.undercontroll.domain.gateway.NotificationGateway;
import com.undercontroll.domain.gateway.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class AfterCommitOrderEventPublisher implements OrderEventPublisher {

    private final NotificationGateway notificationGateway;

    @Override
    public void statusChanged(OrderStatusChangedEvent event) {
        afterCommit(() -> notificationGateway.handleOrderStatusChanged(event));
    }

    @Override
    public void budgetDecided(OrderBudgetDecidedEvent event) {
        afterCommit(() -> notificationGateway.handleOrderBudgetDecided(event));
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            run(action);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                run(action);
            }
        });
    }

    private static void run(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.warn("Order event publish failed cause={}", ex.toString());
        }
    }
}
