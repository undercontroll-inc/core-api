package com.undercontroll.domain.usecase.order;

public interface SendOrderBudgetPort {

    Output execute(Integer orderId);

    record Output(String trackingUrl, String whatsappText, OrderOutput order) {
    }
}
