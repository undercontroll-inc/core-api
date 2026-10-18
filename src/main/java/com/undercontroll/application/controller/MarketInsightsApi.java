package com.undercontroll.application.controller;

import com.undercontroll.application.dto.insights.MarketInsightsResponse;
import com.undercontroll.application.controller.doc.ApiResponseDocumentation.GetApiResponses;
import com.undercontroll.application.controller.doc.ApiResponseDocumentation.PostApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Insights", description = "Monthly AI-generated insights from the ETL pipeline and workshop repairs")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping(value = "/v1/api/insights", produces = MediaType.APPLICATION_JSON_VALUE)
public interface MarketInsightsApi {

    @Operation(summary = "Get the monthly insights batch (read-only; empty list if not generated yet)")
    @GetApiResponses
    @GetMapping
    ResponseEntity<MarketInsightsResponse> getInsights();

    @Operation(summary = "Generate the monthly insights batch (dev profile only; overwrites the existing batch)")
    @PostApiResponses
    @PostMapping
    ResponseEntity<Void> createInsights();
}
