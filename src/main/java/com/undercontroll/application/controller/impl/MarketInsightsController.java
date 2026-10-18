package com.undercontroll.application.controller.impl;

import com.undercontroll.application.controller.MarketInsightsApi;
import com.undercontroll.application.dto.insights.InsightsResponseMapper;
import com.undercontroll.application.dto.insights.MarketInsightsResponse;
import com.undercontroll.domain.usecase.insights.GenerateMonthlyInsightsPort;
import com.undercontroll.domain.usecase.insights.GetMarketInsightsPort;
import com.undercontroll.domain.usecase.insights.InsightGenerationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MarketInsightsController implements MarketInsightsApi {

    private final GetMarketInsightsPort getMarketInsightsPort;
    private final GenerateMonthlyInsightsPort generateMonthlyInsightsPort;
    private final Environment environment;

    @Override
    public ResponseEntity<MarketInsightsResponse> getInsights() {
        GetMarketInsightsPort.Output output = getMarketInsightsPort.execute();
        return ResponseEntity.ok(InsightsResponseMapper.from(output.payload(), output.generatedAt()));
    }

    @Override
    public ResponseEntity<Void> createInsights() {
        if (!environment.matchesProfiles("dev")) {
            return ResponseEntity.notFound().build();
        }
        InsightGenerationResult result = generateMonthlyInsightsPort.execute(true);
        return ResponseEntity.status(httpStatus(result.status())).build();
    }

    private static HttpStatus httpStatus(InsightGenerationResult.Status status) {
        return switch (status) {
            case SUCCESS -> HttpStatus.OK;
            case FAILED -> HttpStatus.BAD_GATEWAY;
            case SKIPPED_NO_BUCKET, SKIPPED_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case SKIPPED_NO_LLM -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }
}
