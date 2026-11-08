package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.ShopProfile;
import com.undercontroll.domain.order.OrderMessages;
import com.undercontroll.domain.usecase.order.SendOrderBudgetPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendOrderBudgetImpl implements SendOrderBudgetPort {

    private final OrderLifecycleService lifecycle;
    private final OrderDetailsAssembler assembler;
    private final OrderGateway orderGateway;
    private final ShopProfile shopProfile;

    @Override
    public Output execute(Integer orderId) {
        Order sent = lifecycle.transition(orderId, OrderStatus.AWAITING_APPROVAL, null, OrderActor.SHOP, null);
        Order order = orderGateway.findById(sent.getId()).orElse(sent);
        String url = shopProfile.trackingUrl(order.getTrackingToken());
        return new Output(url, OrderMessages.whatsapp(order, url), assembler.detail(order, true));
    }
}
