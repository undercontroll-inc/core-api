package com.undercontroll.application.controller;

import com.undercontroll.application.controller.doc.ApiResponseDocumentation.*;
import com.undercontroll.application.dto.demand.CreateDemandRequest;
import com.undercontroll.application.dto.demand.DemandDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Demands", description = "APIs for managing component demands belonging to a repair order")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/v1/api/orders/{orderId}/demands")
public interface DemandApi {

    @Operation(summary = "Create a demand for an order")
    @PostApiResponses
    @PostMapping
    ResponseEntity<DemandDto> createDemand(
            @PathVariable @Parameter(example = "5") Integer orderId,
            @RequestBody CreateDemandRequest request
    );

    @Operation(summary = "List demands of an order, optionally filtered by component")
    @GetApiResponses
    @GetMapping
    ResponseEntity<List<DemandDto>> getDemands(
            @PathVariable @Parameter(example = "5") Integer orderId,
            @RequestParam(required = false) @Parameter(description = "Filter by component id") Integer componentId
    );

    @Operation(summary = "Delete a demand by id")
    @DeleteApiResponses
    @DeleteMapping("/{demandId}")
    ResponseEntity<Void> deleteDemand(
            @PathVariable @Parameter(example = "5") Integer orderId,
            @PathVariable @Parameter(example = "25") Integer demandId
    );

    @Operation(summary = "Delete all demands of an order")
    @DeleteApiResponses
    @DeleteMapping
    ResponseEntity<Void> deleteAllDemands(
            @PathVariable @Parameter(example = "5") Integer orderId
    );
}
