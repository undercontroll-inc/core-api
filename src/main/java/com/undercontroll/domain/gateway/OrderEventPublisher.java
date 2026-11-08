package com.undercontroll.domain.gateway;

import com.undercontroll.domain.event.OrderBudgetDecidedEvent;
import com.undercontroll.domain.event.OrderStatusChangedEvent;

public interface OrderEventPublisher {

    void statusChanged(OrderStatusChangedEvent event);

    void budgetDecided(OrderBudgetDecidedEvent event);
}
