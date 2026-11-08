package com.undercontroll.domain.usecase.order;

public interface CreateTrackingLinkPort {

    Output execute(Integer orderId);

    record Output(String trackingUrl, String whatsappText) {
    }
}
