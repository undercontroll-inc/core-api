package com.undercontroll.domain.usecase.order;

import com.undercontroll.domain.enums.ApprovalDecision;

public interface DecideOrderBudgetPort {

    OrderOutput execute(Input input);

    record Input(Integer orderId, ApprovalDecision decision, String note) {
    }
}
