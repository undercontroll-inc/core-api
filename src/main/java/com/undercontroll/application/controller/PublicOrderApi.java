package com.undercontroll.application.controller;

import com.undercontroll.application.controller.doc.ApiResponseDocumentation.ErrorResponse;
import com.undercontroll.application.dto.order.PublicOrderDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Public order", description = "Customer tracking page data, no login (30 requests/minute per IP)")
@RequestMapping(value = "/v1/public/orders", produces = MediaType.APPLICATION_JSON_VALUE)
public interface PublicOrderApi {

    @Operation(summary = "Order status for the customer link (first name, device, values and status only)")
    @ApiResponse(responseCode = "200", description = "Order",
            content = @Content(schema = @Schema(implementation = PublicOrderDto.class)))
    @ApiResponse(responseCode = "404", description = "Unknown or expired link (code TRACKING_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{token}")
    ResponseEntity<PublicOrderDto> get(@PathVariable String token);
}
