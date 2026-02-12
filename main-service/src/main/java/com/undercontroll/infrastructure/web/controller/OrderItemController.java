package com.undercontroll.infrastructure.web.controller;

import com.undercontroll.infrastructure.web.api.OrderItemApi;
import com.undercontroll.infrastructure.web.dto.CreateOrderItemRequest;
import com.undercontroll.infrastructure.web.dto.OrderItemDto;
import com.undercontroll.infrastructure.web.dto.UpdateOrderItemRequest;
import com.undercontroll.domain.entity.OrderItem;
import com.undercontroll.application.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/api/order-items", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OrderItemController implements OrderItemApi {

    private final OrderItemService service;

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderItemDto> createOrderItem(@RequestBody CreateOrderItemRequest request) {
        OrderItem created = service.createOrderItem(request);
        return ResponseEntity.status(201).body(service.mapToDto(created));
    }

    @Override
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateOrderItem(@RequestBody UpdateOrderItemRequest request) {
        service.updateOrderItem(request);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping
    public ResponseEntity<List<OrderItemDto>> getOrderItems() {
        var items = service.getOrderItems();
        return items.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(items);
    }

    @Override
    @GetMapping("/{orderItemId}")
    public ResponseEntity<OrderItemDto> getOrderItemById(@PathVariable Integer orderItemId) {
        var dto = service.getOrderItemById(orderItemId);
        return ResponseEntity.ok(dto);
    }

    @Override
    @DeleteMapping("/{orderItemId}")
    public ResponseEntity<Void> deleteOrderItem(@PathVariable Integer orderItemId) {
        service.deleteOrderItem(orderItemId);
        return ResponseEntity.ok().build();
    }
}

