package com.undercontroll.application.controller.impl;

import com.undercontroll.application.controller.MarketInsightsApi;
import com.undercontroll.application.dto.insights.InsightsResponseMapper;
import com.undercontroll.application.dto.insights.MarketInsightsResponse;
import com.undercontroll.domain.usecase.insights.GetMarketInsightsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MarketInsightsController implements MarketInsightsApi {

    private final GetMarketInsightsPort getMarketInsightsPort;

    @Override
    public ResponseEntity<MarketInsightsResponse> getInsights() {
        GetMarketInsightsPort.Output output = getMarketInsightsPort.execute();
        return ResponseEntity.ok(InsightsResponseMapper.from(output.payload(), output.generatedAt()));
    }
}
