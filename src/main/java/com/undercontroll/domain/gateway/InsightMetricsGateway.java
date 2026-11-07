package com.undercontroll.domain.gateway;

import java.time.Duration;
import java.time.Instant;

public interface InsightMetricsGateway {

    void recordJob(String outcome, Duration duration);

    void recordDraftValidation(boolean repair, boolean valid);

    void snapshotPublished(Instant publishedAt);

    void insightsPublished(Instant publishedAt);
}
