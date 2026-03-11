package com.undercontroll.presentation.controller;

import com.undercontroll.infrastructure.config.ApiResponseDocumentation.*;
import com.undercontroll.presentation.dto.CreateOrderItemRequest;
import com.undercontroll.application.dto.OrderItemDto;
import com.undercontroll.presentation.dto.UpdateOrderItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Order Items", description = "APIs para gerenciamento de itens de pedido")
@SecurityRequirement(name = "Bearer Authentication")
public interface OrderItemApi {

    @Operation(summary = "Criar item de pedido")
    @PostApiResponses
    ResponseEntity<OrderItemDto> createOrderItem(CreateOrderItemRequest request);

    @Operation(summary = "Atualizar item de pedido")
    @PutApiResponses
    ResponseEntity<Void> updateOrderItem(UpdateOrderItemRequest request);

    @Operation(summary = "Listar todos os itens de pedido")
    @GetApiResponses
    ResponseEntity<List<OrderItemDto>> getOrderItems();

    @Operation(summary = "Buscar item de pedido por ID")
    @GetApiResponses
    ResponseEntity<OrderItemDto> getOrderItemById(@Parameter(example = "1") Integer orderItemId);

    @Operation(summary = "Deletar item de pedido")
    @DeleteApiResponses
    ResponseEntity<Void> deleteOrderItem(@Parameter(example = "1") Integer orderItemId);
}

