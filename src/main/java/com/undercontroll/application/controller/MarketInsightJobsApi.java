package com.undercontroll.application.controller;

import com.undercontroll.application.controller.doc.ApiResponseDocumentation.ErrorResponse;
import com.undercontroll.application.dto.insights.InsightJobResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Insights", description = "Monthly AI-generated insights from the ETL pipeline and workshop repairs")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping(value = "/v1/api/market/insights/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
public interface MarketInsightJobsApi {

    @Operation(
            summary = "Request insight generation for the published market snapshot",
            description = "Idempotent per snapshot, prompt version and model configuration. Creates the job when missing "
                    + "and requeues it when it failed; the background worker runs it. Poll GET /v1/api/insights for the state."
    )
    @ApiResponse(
            responseCode = "202",
            description = "Job accepted (new, pending, running, already published or requeued)",
            content = @Content(schema = @Schema(implementation = InsightJobResponse.class))
    )
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Only ADMINISTRATOR",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "No snapshot published by the ETL yet (code AWAITING_DATA)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503",
            description = "Insights disabled (code INSIGHTS_DISABLED) or market views unavailable (code MARKET_DATA_UNAVAILABLE)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    ResponseEntity<InsightJobResponse> requestJob();
}
