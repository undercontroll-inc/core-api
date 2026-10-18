package com.undercontroll.application.controller.impl;

import com.undercontroll.application.controller.MarketInsightJobsApi;
import com.undercontroll.application.dto.insights.InsightJobResponse;
import com.undercontroll.domain.usecase.insights.RequestInsightJobPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MarketInsightJobsController implements MarketInsightJobsApi {

    private final RequestInsightJobPort requestInsightJobPort;

    @Override
    public ResponseEntity<InsightJobResponse> requestJob() {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(InsightJobResponse.from(requestInsightJobPort.execute()));
    }
}
