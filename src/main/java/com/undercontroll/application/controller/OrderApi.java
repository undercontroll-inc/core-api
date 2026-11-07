package com.undercontroll.application.controller;

import com.undercontroll.application.controller.doc.ApiResponseDocumentation.*;
import com.undercontroll.application.dto.order.CreateOrderRequest;
import com.undercontroll.application.dto.order.GetAllOrdersResponse;
import com.undercontroll.application.dto.order.GetOrderByIdResponse;
import com.undercontroll.application.dto.order.OrderEnrichedDto;
import com.undercontroll.application.dto.order.UpdateOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Orders", description = "APIs for managing repair orders")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping(value = "/v1/api/orders")
public interface OrderApi {

    @Operation(summary = "Create a new order")
    @PostApiResponses
    @PostMapping
    ResponseEntity<OrderEnrichedDto> createOrder(@RequestBody CreateOrderRequest request);

    @Operation(summary = "Update an order")
    @PatchApiResponses
    @PatchMapping(value = "/{orderId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> updateOrder(
            @PathVariable @Parameter(example = "1") Integer orderId,
            @RequestBody UpdateOrderRequest request
    );

    @Operation(summary = "List orders, optionally filtered by user (paginated)")
    @GetApiResponses
    @GetMapping
    ResponseEntity<GetAllOrdersResponse> getOrders(
            @RequestParam(required = false) @Parameter(description = "Filter by the owning user id. Customers may only filter by their own id.") Integer userId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    );

    @Operation(summary = "Get an order by id")
    @GetApiResponses
    @GetMapping("/{orderId}")
    ResponseEntity<GetOrderByIdResponse> getOrderById(@PathVariable @Parameter(example = "1") Integer orderId);

    @Operation(summary = "Delete an order")
    @DeleteApiResponses
    @DeleteMapping("/{orderId}")
    ResponseEntity<Void> deleteOrder(@PathVariable @Parameter(example = "1") Integer orderId);

    @Operation(summary = "Export an order as a PDF report")
    @ApiResponse(
            responseCode = "200",
            description = "PDF report generated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary"))
    )
    @GetMapping("/{orderId}/export")
    ResponseEntity<byte[]> exportOrder(@PathVariable @Parameter(example = "1") Integer orderId);
}
