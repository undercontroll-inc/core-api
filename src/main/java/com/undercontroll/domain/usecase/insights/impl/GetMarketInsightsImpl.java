package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.gateway.MarketInsightGateway;
import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.model.MarketMonthlyInsight;
import com.undercontroll.domain.usecase.insights.GetMarketInsightsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetMarketInsightsImpl implements GetMarketInsightsPort {

    private final MarketViewGateway marketViewGateway;
    private final MarketInsightGateway marketInsightGateway;

    @Override
    @Cacheable(value = "marketInsights", key = "'current'")
    public Output execute() {
        return marketViewGateway.findCurrentBucketKey()
                .flatMap(marketInsightGateway::findSuccessfulByBucketKey)
                .map(this::toOutput)
                .orElseGet(Output::empty);
    }

    private Output toOutput(MarketMonthlyInsight insight) {
        if (insight.getPayload() == null) {
            log.warn("Stored insights for bucket {} have no readable payload", insight.getBucketKey());
            return Output.empty();
        }
        String generatedAt = insight.getGeneratedAt() == null ? null : insight.getGeneratedAt().toString();
        return new Output(insight.getPayload(), generatedAt);
    }
}
