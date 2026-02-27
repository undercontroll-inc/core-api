package com.undercontroll.domain.port.out;

import com.undercontroll.domain.model.OrderItem;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepositoryPort {

    OrderItem save(OrderItem orderItem);

    void deleteById(Integer id);

    Optional<OrderItem> findById(Integer id);

    List<OrderItem> findAll();

}
