package com.undercontroll.application.controller;

import com.undercontroll.application.controller.doc.ApiResponseDocumentation.ErrorResponse;
import com.undercontroll.application.dto.stockimport.StockImportDto;
import com.undercontroll.application.dto.storage.UploadReference;
import com.undercontroll.application.dto.storage.UploadRequest;
import com.undercontroll.application.dto.storage.UploadTicketDto;
import com.undercontroll.application.dto.stockimport.StockImportReviewRequest;
import com.undercontroll.application.dto.stockimport.StockImportSummaryDto;
import com.undercontroll.domain.enums.StockImportStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Stock imports", description = "Read a nota fiscal with AI and add its items to stock")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping(value = "/v1/api/stock-imports", produces = MediaType.APPLICATION_JSON_VALUE)
public interface StockImportApi {

    @Operation(
            summary = "Get a presigned URL to upload the invoice (photo, PDF or NF-e XML)",
            description = "Step 1. PUT the file to uploadUrl with the returned headers, then POST /v1/api/stock-imports with the uploadId."
    )
    @ApiResponse(responseCode = "201", description = "Upload ticket",
            content = @Content(schema = @Schema(implementation = UploadTicketDto.class)))
    @ApiResponse(responseCode = "400", description = "Unsupported type or over 10MB (code STOCK_IMPORT_INVALID)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "AI extraction disabled; XML still works (code STOCK_IMPORT_UNAVAILABLE)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/uploads", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<UploadTicketDto> requestUpload(@RequestBody UploadRequest request);

    @Operation(
            summary = "Create the import from an uploaded file (step 2, idempotent per uploadId)",
            description = "Extraction runs in the background. Poll GET /v1/api/stock-imports/{importId} until status leaves PROCESSING."
    )
    @ApiResponse(responseCode = "202", description = "Import created with status PROCESSING",
            content = @Content(schema = @Schema(implementation = StockImportDto.class)))
    @ApiResponse(responseCode = "400",
            description = "Upload missing or expired, content that contradicts the declared type, or PDF over 5 pages (code STOCK_IMPORT_INVALID)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Only ADMINISTRATOR",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "AI extraction disabled; XML still works (code STOCK_IMPORT_UNAVAILABLE)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<StockImportDto> create(@RequestBody UploadReference request);

    @Operation(summary = "List recent imports, newest first")
    @ApiResponse(responseCode = "200", description = "Imports",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = StockImportSummaryDto.class))))
    @GetMapping
    ResponseEntity<List<StockImportSummaryDto>> list(
            @RequestParam(required = false) StockImportStatus status,
            @Parameter(description = "1 to 50, default 20") @RequestParam(required = false) Integer limit
    );

    @Operation(summary = "Get an import with its extracted items")
    @ApiResponse(responseCode = "200", description = "Import",
            content = @Content(schema = @Schema(implementation = StockImportDto.class)))
    @ApiResponse(responseCode = "404", description = "Not found (code STOCK_IMPORT_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{importId}")
    ResponseEntity<StockImportDto> get(@PathVariable Long importId);

    @Operation(summary = "Save the reviewed items as a draft (only READY)")
    @ApiResponse(responseCode = "200", description = "Draft saved",
            content = @Content(schema = @Schema(implementation = StockImportDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid review (code VALIDATION_ERROR with errors[].field)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found (code STOCK_IMPORT_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Not READY (code STOCK_IMPORT_INVALID_STATE)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping(value = "/{importId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<StockImportDto> update(@PathVariable Long importId, @RequestBody StockImportReviewRequest request);

    @Operation(
            summary = "Confirm the reviewed items and update stock",
            description = "One transaction: ADD_TO_EXISTING increments the component, CREATE_NEW creates one, IGNORE skips."
    )
    @ApiResponse(responseCode = "200", description = "Import CONFIRMED with a result per item",
            content = @Content(schema = @Schema(implementation = StockImportDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid review (code VALIDATION_ERROR with errors[].field)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found (code STOCK_IMPORT_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Not READY, e.g. already confirmed (code STOCK_IMPORT_INVALID_STATE)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "Unknown componentId (code COMPONENT_NOT_FOUND with errors[].field)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/{importId}/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<StockImportDto> confirm(@PathVariable Long importId, @RequestBody StockImportReviewRequest request);

    @Operation(summary = "Run extraction again for a FAILED import")
    @ApiResponse(responseCode = "202", description = "Import back to PROCESSING",
            content = @Content(schema = @Schema(implementation = StockImportDto.class)))
    @ApiResponse(responseCode = "404", description = "Not found (code STOCK_IMPORT_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Not FAILED (code STOCK_IMPORT_INVALID_STATE)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{importId}/retry")
    ResponseEntity<StockImportDto> retry(@PathVariable Long importId);

    @Operation(summary = "Cancel an import that was not confirmed")
    @ApiResponse(responseCode = "204", description = "Canceled (idempotent)")
    @ApiResponse(responseCode = "404", description = "Not found (code STOCK_IMPORT_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Already CONFIRMED (code STOCK_IMPORT_INVALID_STATE)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{importId}")
    ResponseEntity<Void> cancel(@PathVariable Long importId);
}
