package com.undercontroll.domain.usecase.order;

import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;

public interface TransitionOrderPort {

    OrderOutput execute(Input input);

    record Input(Integer orderId, OrderStatus to, OrderOutcome outcome, String note) {
    }
}
