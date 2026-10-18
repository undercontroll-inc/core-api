package com.undercontroll.domain.usecase.order;

import java.util.Optional;

public interface GetOrderByIdPort {
    Optional<OrderOutput> execute(Integer orderId);
}
