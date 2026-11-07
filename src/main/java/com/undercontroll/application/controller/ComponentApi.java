package com.undercontroll.application.controller;

import com.undercontroll.application.controller.doc.ApiResponseDocumentation.*;
import com.undercontroll.application.dto.component.ComponentDto;
import com.undercontroll.application.dto.component.RegisterComponentRequest;
import com.undercontroll.application.dto.component.UpdateComponentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Components", description = "APIs for managing electronic components and materials")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping(value = "/v1/api/components", produces = MediaType.APPLICATION_JSON_VALUE)
public interface ComponentApi {

    @Operation(summary = "Create a new component")
    @PostApiResponses
    @PostMapping
    ResponseEntity<ComponentDto> register(@RequestBody RegisterComponentRequest request);

    @Operation(summary = "List components, optionally filtered by category or name")
    @GetApiResponses
    @GetMapping
    ResponseEntity<List<ComponentDto>> findAll(
            @RequestParam(required = false) @Parameter(description = "Filter by category") String category,
            @RequestParam(required = false) @Parameter(description = "Filter by name") String name
    );

    @Operation(summary = "Get a component by id")
    @GetApiResponses
    @GetMapping("/{componentId}")
    ResponseEntity<ComponentDto> getById(@PathVariable @Parameter(example = "1") Integer componentId);

    @Operation(summary = "Update a component")
    @PutApiResponses
    @PutMapping("/{componentId}")
    ResponseEntity<ComponentDto> updateComponent(
            @RequestBody UpdateComponentRequest request,
            @PathVariable @Parameter(example = "1") Integer componentId
    );

    @Operation(summary = "Delete a component")
    @DeleteApiResponses
    @DeleteMapping("/{componentId}")
    ResponseEntity<Void> deleteComponent(@PathVariable @Parameter(example = "1") Integer componentId);
}
