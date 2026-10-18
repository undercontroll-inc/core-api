package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.exception.UnauthorizedOrderOperation;
import com.undercontroll.domain.gateway.CurrentUserAdminPort;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.usecase.order.GetOrdersPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetOrdersImpl implements GetOrdersPort {

    private final OrderListingQuery orderListingQuery;
    private final CurrentUserAdminPort currentUserAdminPort;
    private final CurrentUserIdPort currentUserIdPort;

    @Override
    public Output execute(Integer userId, Integer page, Integer size) {
        boolean administrator = currentUserAdminPort.isAdministrator();
        if (!administrator) {
            requireOwnOrders(userId);
        }
        return orderListingQuery.list(userId, page, size, administrator);
    }

    private void requireOwnOrders(Integer userId) {
        if (userId == null || !userId.equals(currentUserIdPort.require())) {
            throw new UnauthorizedOrderOperation("Customers can only list their own orders");
        }
    }
}
