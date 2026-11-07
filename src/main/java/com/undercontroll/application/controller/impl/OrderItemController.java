package com.undercontroll.application.controller.impl;

import com.undercontroll.domain.usecase.order_item.*;
import com.undercontroll.application.controller.OrderItemApi;
import com.undercontroll.application.dto.orderitem.CreateOrderItemRequest;
import com.undercontroll.application.dto.orderitem.OrderItemDto;
import com.undercontroll.application.dto.orderitem.UpdateOrderItemRequest;
import com.undercontroll.application.mapper.OrderItemDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderItemController implements OrderItemApi {

    private final CreateOrderItemPort createOrderItemPort;
    private final UpdateOrderItemPort updateOrderItemPort;
    private final GetOrderItemsPort getOrderItemsPort;
    private final GetOrderItemByIdPort getOrderItemByIdPort;
    private final DeleteOrderItemPort deleteOrderItemPort;
    private final OrderItemDtoMapper mapper;

    @Override
    public ResponseEntity<OrderItemDto> createOrderItem(Integer orderId, CreateOrderItemRequest request) {
        return ResponseEntity.status(201).body(mapper.toDto(createOrderItemPort.execute(orderId, mapper.toInput(request))));
    }

    @Override
    public ResponseEntity<Void> updateOrderItem(Integer orderId, Integer orderItemId, UpdateOrderItemRequest request) {
        updateOrderItemPort.execute(orderId, orderItemId, mapper.toInput(request));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<OrderItemDto>> getOrderItems(Integer orderId) {
        return ResponseEntity.ok(getOrderItemsPort.execute(orderId).stream().map(mapper::toDto).toList());
    }

    @Override
    public ResponseEntity<OrderItemDto> getOrderItemById(Integer orderId, Integer orderItemId) {
        return getOrderItemByIdPort.execute(orderId, orderItemId)
                .map(mapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteOrderItem(Integer orderId, Integer orderItemId) {
        deleteOrderItemPort.execute(orderId, orderItemId);
        return ResponseEntity.noContent().build();
    }
}
