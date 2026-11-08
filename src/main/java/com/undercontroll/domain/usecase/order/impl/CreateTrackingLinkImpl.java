package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.ShopProfile;
import com.undercontroll.domain.order.OrderMessages;
import com.undercontroll.domain.usecase.order.CreateTrackingLinkPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateTrackingLinkImpl implements CreateTrackingLinkPort {

    private final OrderLifecycleService lifecycle;
    private final OrderGateway orderGateway;
    private final ShopProfile shopProfile;

    @Override
    public Output execute(Integer orderId) {
        Order tokenized = lifecycle.ensureTrackingToken(orderId);
        Order order = orderGateway.findById(tokenized.getId()).orElse(tokenized);
        String url = shopProfile.trackingUrl(order.getTrackingToken());
        return new Output(url, OrderMessages.whatsapp(order, url));
    }
}
