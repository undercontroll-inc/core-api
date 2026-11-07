package com.undercontroll.application.controller;

import com.undercontroll.application.controller.doc.ApiResponseDocumentation.*;
import com.undercontroll.application.dto.orderitem.CreateOrderItemRequest;
import com.undercontroll.application.dto.orderitem.OrderItemDto;
import com.undercontroll.application.dto.orderitem.UpdateOrderItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Order Items", description = "APIs for managing appliances (order items) belonging to a repair order")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/v1/api/orders/{orderId}/items")
public interface OrderItemApi {

    @Operation(summary = "Create an order item")
    @PostApiResponses
    @PostMapping
    ResponseEntity<OrderItemDto> createOrderItem(
            @PathVariable @Parameter(example = "1") Integer orderId,
            @RequestBody CreateOrderItemRequest request
    );

    @Operation(summary = "Update an order item")
    @PatchApiResponses
    @PatchMapping("/{orderItemId}")
    ResponseEntity<Void> updateOrderItem(
            @PathVariable @Parameter(example = "1") Integer orderId,
            @PathVariable @Parameter(example = "10") Integer orderItemId,
            @RequestBody UpdateOrderItemRequest request
    );

    @Operation(summary = "List items of an order")
    @GetApiResponses
    @GetMapping
    ResponseEntity<List<OrderItemDto>> getOrderItems(
            @PathVariable @Parameter(example = "1") Integer orderId
    );

    @Operation(summary = "Get an order item by id")
    @GetApiResponses
    @GetMapping("/{orderItemId}")
    ResponseEntity<OrderItemDto> getOrderItemById(
            @PathVariable @Parameter(example = "1") Integer orderId,
            @PathVariable @Parameter(example = "10") Integer orderItemId
    );

    @Operation(summary = "Delete an order item")
    @DeleteApiResponses
    @DeleteMapping("/{orderItemId}")
    ResponseEntity<Void> deleteOrderItem(
            @PathVariable @Parameter(example = "1") Integer orderId,
            @PathVariable @Parameter(example = "10") Integer orderItemId
    );
}
