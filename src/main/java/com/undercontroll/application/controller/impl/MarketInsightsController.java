package com.undercontroll.application.controller.impl;

import com.undercontroll.application.controller.MarketInsightsApi;
import com.undercontroll.application.dto.insights.InsightsResponseMapper;
import com.undercontroll.application.dto.insights.MarketInsightsResponse;
import com.undercontroll.domain.usecase.insights.EnqueueInsightJobPort;
import com.undercontroll.domain.usecase.insights.GetMarketInsightsPort;
import com.undercontroll.domain.usecase.insights.ProcessInsightJobsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MarketInsightsController implements MarketInsightsApi {

    private final GetMarketInsightsPort getMarketInsightsPort;
    private final EnqueueInsightJobPort enqueueInsightJobPort;
    private final ProcessInsightJobsPort processInsightJobsPort;
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
        EnqueueInsightJobPort.Result result = enqueueInsightJobPort.execute(true);
        return switch (result.status()) {
            case AWAITING_DATA -> ResponseEntity.status(HttpStatus.CONFLICT).build();
            case DISABLED -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            case ENQUEUED -> {
                processInsightJobsPort.execute();
                yield ResponseEntity.ok().build();
            }
        };
    }
}
