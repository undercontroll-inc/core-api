package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.gateway.InsightPublicationGateway;
import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.model.insight.MarketInsightPublication;
import com.undercontroll.domain.usecase.insights.GetMarketInsightsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetMarketInsightsImpl implements GetMarketInsightsPort {

    private final MarketViewGateway marketViewGateway;
    private final InsightPublicationGateway insightPublicationGateway;

    @Override
    @Cacheable(value = "marketInsights", key = "'current'", unless = "#result.payload() == null")
    public Output execute() {
        Optional<String> bucket = marketViewGateway.findCurrentBucketKey();
        if (bucket.isEmpty()) {
            return Output.empty();
        }
        return insightPublicationGateway.findLatest()
                .filter(publication -> bucket.get().equals(publication.bucketKey()))
                .map(this::toOutput)
                .orElseGet(Output::empty);
    }

    private Output toOutput(MarketInsightPublication publication) {
        if (publication.payload() == null) {
            log.warn("Published insights for bucket {} have no readable payload", publication.bucketKey());
            return Output.empty();
        }
        String generatedAt = publication.publishedAt() == null ? null : publication.publishedAt().toString();
        return new Output(publication.payload(), generatedAt);
    }
}
