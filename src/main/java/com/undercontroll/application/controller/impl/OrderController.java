package com.undercontroll.application.controller.impl;

import com.undercontroll.application.dto.order.CreateOrderRequest;
import com.undercontroll.application.dto.order.GetAllOrdersResponse;
import com.undercontroll.application.dto.order.GetOrderByIdResponse;
import com.undercontroll.application.dto.order.OrderEnrichedDto;
import com.undercontroll.application.dto.order.UpdateOrderRequest;
import com.undercontroll.application.mapper.OrderDtoMapper;
import com.undercontroll.domain.usecase.order.*;
import com.undercontroll.application.controller.OrderApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final CreateOrderPort createOrderPort;
    private final UpdateOrderPort updateOrderPort;
    private final GetOrdersPort getOrdersPort;
    private final GetOrderByIdPort getOrderByIdPort;
    private final DeleteOrderPort deleteOrderPort;
    private final ExportOrderPort exportOrderPort;
    private final OrderDtoMapper mapper;

    @Override
    public ResponseEntity<OrderEnrichedDto> createOrder(CreateOrderRequest request) {
        return ResponseEntity.status(201).body(mapper.toDto(createOrderPort.execute(mapper.toInput(request))));
    }

    @Override
    public ResponseEntity<Void> updateOrder(Integer orderId, UpdateOrderRequest request) {
        updateOrderPort.execute(orderId, mapper.toInput(request));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<GetAllOrdersResponse> getOrders(Integer userId, Integer page, Integer size) {
        return ResponseEntity.ok(mapper.toResponse(getOrdersPort.execute(userId, page, size)));
    }

    @Override
    public ResponseEntity<GetOrderByIdResponse> getOrderById(Integer orderId) {
        return getOrderByIdPort.execute(orderId)
                .map(order -> new GetOrderByIdResponse(mapper.toDto(order)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteOrder(Integer orderId) {
        deleteOrderPort.execute(orderId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<byte[]> exportOrder(Integer orderId) {
        byte[] pdfData = exportOrderPort.execute(orderId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}
