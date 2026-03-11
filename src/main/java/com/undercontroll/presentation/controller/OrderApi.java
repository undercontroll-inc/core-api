package com.undercontroll.presentation.controller;

import com.undercontroll.infrastructure.config.ApiResponseDocumentation.*;
import com.undercontroll.application.dto.*;
import com.undercontroll.domain.model.Order;
import com.undercontroll.presentation.dto.CreateOrderRequest;
import com.undercontroll.presentation.dto.GetAllOrdersResponse;
import com.undercontroll.presentation.dto.UpdateOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Orders", description = "APIs para gerenciamento de pedidos e ordens de serviço")
@SecurityRequirement(name = "Bearer Authentication")
public interface OrderApi {

    @Operation(summary = "Criar novo pedido")
    @PostApiResponses
    ResponseEntity<Order> createOrder(CreateOrderRequest request);

    @Operation(summary = "Atualizar pedido")
    @PutApiResponses
    ResponseEntity<Void> updateOrder(@Parameter(example = "1") Integer id, UpdateOrderRequest request);

    @Operation(summary = "Listar todos os pedidos")
    @GetApiResponses
    ResponseEntity<GetAllOrdersResponse> getOrders();

    @Operation(summary = "Buscar pedido por ID")
    @GetApiResponses
    ResponseEntity<GetOrderByIdResponse> getOrderById(@Parameter(example = "1") Integer orderId);

    @Operation(summary = "Deletar pedido")
    @DeleteApiResponses
    ResponseEntity<Void> deleteOrder(@Parameter(example = "1") Integer orderId);

    @Operation(summary = "Buscar pedidos por ID do usuário")
    @GetApiResponses
    ResponseEntity<GetOrdersByUserIdResponse> getOrdersByUserId(@Parameter(example = "1") Integer userId);
}

