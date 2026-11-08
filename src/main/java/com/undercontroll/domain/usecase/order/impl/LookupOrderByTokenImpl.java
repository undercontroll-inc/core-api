package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.order.TrackingTokens;
import com.undercontroll.domain.usecase.order.LookupOrderByTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LookupOrderByTokenImpl implements LookupOrderByTokenPort {

    private final OrderGateway orderGateway;

    @Override
    public Integer execute(String token) {
        if (!TrackingTokens.wellFormed(token)) {
            throw new OrderNotFoundException("No order for this tracking token");
        }
        return orderGateway.findByTrackingToken(token)
                .map(Order::getId)
                .orElseThrow(() -> new OrderNotFoundException("No order for this tracking token"));
    }
}
