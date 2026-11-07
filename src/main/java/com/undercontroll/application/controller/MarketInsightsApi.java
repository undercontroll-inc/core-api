package com.undercontroll.application.controller;

import com.undercontroll.application.dto.insights.MarketInsightsResponse;
import com.undercontroll.application.controller.doc.ApiResponseDocumentation.GetApiResponses;
import com.undercontroll.application.controller.doc.ApiResponseDocumentation.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Insights", description = "Monthly AI-generated insights from the ETL pipeline and workshop repairs")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping(value = "/v1/api/insights", produces = MediaType.APPLICATION_JSON_VALUE)
public interface MarketInsightsApi {

    @Operation(
            summary = "Get the insights of the published market snapshot with an explicit state",
            description = "Always 200 with a state: READY (current), STALE (older publication, nothing running), "
                    + "AWAITING_DATA (ETL has not published yet, empty lists), GENERATING (job pending or running; "
                    + "an older publication may be served) or FAILED (failureReason set; an older publication may be "
                    + "served). Compare requestedBucketKey with availableBucketKey to show stale content. Only READY "
                    + "responses are cached."
    )
    @GetApiResponses
    @ApiResponse(
            responseCode = "500",
            description = "Published payload is unreadable (code INSIGHT_PAYLOAD_UNREADABLE)",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "503",
            description = "Market views unavailable (code MARKET_DATA_UNAVAILABLE); not cached, retry later",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @GetMapping
    ResponseEntity<MarketInsightsResponse> getInsights();
}
