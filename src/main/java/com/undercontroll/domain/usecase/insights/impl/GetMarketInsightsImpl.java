package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.gateway.InsightJobGateway;
import com.undercontroll.domain.gateway.InsightPublicationGateway;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.gateway.MarketSnapshotGateway;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.model.insight.InsightReadState;
import com.undercontroll.domain.model.insight.MarketInsightPublication;
import com.undercontroll.domain.model.market.PublishedMarketSnapshot;
import com.undercontroll.domain.usecase.insights.GetMarketInsightsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetMarketInsightsImpl implements GetMarketInsightsPort {

    static final String INSIGHTS_DISABLED = "INSIGHTS_DISABLED";

    private final MarketSnapshotGateway marketSnapshotGateway;
    private final InsightPublicationGateway insightPublicationGateway;
    private final InsightJobGateway insightJobGateway;
    private final ObjectProvider<InsightsLlmGateway> insightsLlmGateway;

    @Override
    @Cacheable(
            value = "marketInsights",
            key = "'current'",
            unless = "#result.state() != T(com.undercontroll.domain.model.insight.InsightReadState).READY"
    )
    public Output execute() {
        Optional<PublishedMarketSnapshot> published = marketSnapshotGateway.findPublished();
        if (published.isEmpty()) {
            return Output.awaitingData();
        }
        PublishedMarketSnapshot snapshot = published.get();
        Optional<MarketInsightPublication> latest = insightPublicationGateway.findLatest();
        if (latest.filter(publication -> publication.snapshotId() == snapshot.snapshotId()).isPresent()) {
            return output(InsightReadState.READY, snapshot, latest, null);
        }
        Optional<InsightJob> job = insightJobGateway.findLatestForSnapshot(snapshot.snapshotId());
        if (job.filter(InsightJob::active).isPresent()) {
            return output(InsightReadState.GENERATING, snapshot, latest, null);
        }
        if (job.isPresent()) {
            return output(InsightReadState.FAILED, snapshot, latest, reason(job.get().lastError()));
        }
        if (insightsLlmGateway.getIfAvailable() != null) {
            return output(InsightReadState.GENERATING, snapshot, latest, null);
        }
        return latest.isPresent()
                ? output(InsightReadState.STALE, snapshot, latest, null)
                : output(InsightReadState.FAILED, snapshot, latest, INSIGHTS_DISABLED);
    }

    private static Output output(
            InsightReadState state,
            PublishedMarketSnapshot snapshot,
            Optional<MarketInsightPublication> publication,
            String failureReason
    ) {
        return new Output(
                state,
                snapshot.bucketKey(),
                snapshot.publishedAt(),
                publication.map(MarketInsightPublication::bucketKey).orElse(null),
                publication.map(MarketInsightPublication::publishedAt).orElse(null),
                failureReason,
                publication.map(MarketInsightPublication::payload).orElse(null)
        );
    }

    static String reason(String lastError) {
        if (lastError == null || lastError.isBlank()) {
            return null;
        }
        int separator = lastError.indexOf(':');
        return separator > 0 ? lastError.substring(0, separator) : lastError;
    }
}
