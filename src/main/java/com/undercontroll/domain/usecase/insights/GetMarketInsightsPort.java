package com.undercontroll.domain.usecase.insights;

import com.undercontroll.domain.model.insight.InsightReadState;
import com.undercontroll.domain.model.insight.InsightsPayload;

import java.time.Instant;

public interface GetMarketInsightsPort {

    Output execute();

    record Output(
            InsightReadState state,
            String requestedBucketKey,
            Instant snapshotPublishedAt,
            String availableBucketKey,
            Instant generatedAt,
            String failureReason,
            InsightsPayload payload
    ) {

        public static Output awaitingData() {
            return new Output(InsightReadState.AWAITING_DATA, null, null, null, null, null, null);
        }
    }
}
