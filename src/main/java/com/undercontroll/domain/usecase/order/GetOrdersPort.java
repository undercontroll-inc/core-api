package com.undercontroll.domain.usecase.order;

import java.util.List;

public interface GetOrdersPort {

    Output execute(Integer userId, Integer page, Integer size);

    record Output(List<OrderOutput> data, long totalElements, int totalPages, int page, int size) {
    }
}
