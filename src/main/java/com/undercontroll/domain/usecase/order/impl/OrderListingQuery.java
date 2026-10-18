package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.PaginatedResult;
import com.undercontroll.domain.usecase.order.GetOrdersPort;
import com.undercontroll.domain.usecase.order.OrderOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderListingQuery {

    private final OrderGateway orderGateway;

    @Cacheable(value = "orders", key = "#userId + '-' + #page + '-' + #size + '-' + #includeTechnical")
    public GetOrdersPort.Output list(Integer userId, Integer page, Integer size, boolean includeTechnical) {
        if (userId != null) {
            log.debug("Fetching orders for userId: {}", userId);

            List<OrderOutput> orders = orderGateway.findByUserId(userId).stream()
                    .map(order -> OrderOutput.from(order, includeTechnical))
                    .toList();

            return new GetOrdersPort.Output(orders, orders.size(), 1, 0, orders.size());
        }

        log.debug("Fetching all orders");

        PaginatedResult<Order> result = orderGateway.findAllPaginated(page, size);

        List<OrderOutput> enrichedOrders = result.content().stream()
                .map(order -> OrderOutput.from(order, includeTechnical))
                .toList();

        int totalPages = size > 0
                ? (int) Math.ceil((double) result.totalElements() / size)
                : 0;

        return new GetOrdersPort.Output(enrichedOrders, result.totalElements(), totalPages, page, size);
    }
}
