package com.undercontroll.infrastructure.web.controller;

import com.undercontroll.domain.port.in.*;
import com.undercontroll.infrastructure.web.api.OrderItemApi;
import com.undercontroll.infrastructure.web.dto.CreateOrderItemRequest;
import com.undercontroll.application.dto.OrderItemDto;
import com.undercontroll.infrastructure.web.dto.UpdateOrderItemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/api/order-items", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OrderItemController implements OrderItemApi {

    private final CreateOrderItemPort createOrderItemPort;
    private final UpdateOrderItemPort updateOrderItemPort;
    private final GetOrderItemsPort getOrderItemsPort;
    private final GetOrderItemByIdPort getOrderItemByIdPort;
    private final DeleteOrderItemPort deleteOrderItemPort;

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderItemDto> createOrderItem(@RequestBody CreateOrderItemRequest request) {
        var output = createOrderItemPort.execute(new CreateOrderItemPort.Input(
                request.brand(),
                request.model(),
                request.type(),
                request.imageUrl(),
                request.observation(),
                request.volt(),
                request.series(),
                request.laborValue()
        ));
        return ResponseEntity.status(201).body(new OrderItemDto(
                output.id(),
                null,
                output.model(),
                output.type(),
                output.brand(),
                null,
                null,
                null,
                output.laborValue(),
                null
        ));
    }

    @Override
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateOrderItem(@RequestBody UpdateOrderItemRequest request) {
        updateOrderItemPort.execute(new UpdateOrderItemPort.Input(
                request.id(),
                request.imageUrl(),
                request.labor(),
                request.observation(),
                request.volt(),
                request.series(),
                request.type(),
                request.brand(),
                request.model(),
                request.completedAt()
        ));
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping
    public ResponseEntity<List<OrderItemDto>> getOrderItems() {
        var output = getOrderItemsPort.execute(new GetOrderItemsPort.Input());
        return output.orderItems().isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(output.orderItems());
    }

    @Override
    @GetMapping("/{orderItemId}")
    public ResponseEntity<OrderItemDto> getOrderItemById(@PathVariable Integer orderItemId) {
        var output = getOrderItemByIdPort.execute(new GetOrderItemByIdPort.Input(orderItemId));
        return output.orderItem() != null ? ResponseEntity.ok(output.orderItem()) : ResponseEntity.notFound().build();
    }

    @Override
    @DeleteMapping("/{orderItemId}")
    public ResponseEntity<Void> deleteOrderItem(@PathVariable Integer orderItemId) {
        deleteOrderItemPort.execute(new DeleteOrderItemPort.Input(orderItemId));
        return ResponseEntity.ok().build();
    }
}
