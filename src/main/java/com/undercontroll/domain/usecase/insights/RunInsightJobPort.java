package com.undercontroll.domain.usecase.insights;

import com.undercontroll.domain.model.insight.InsightJob;

public interface RunInsightJobPort {

    Outcome execute(InsightJob job, String owner);

    enum Outcome {
        PUBLISHED,
        RESCHEDULED,
        FAILED,
        LEASE_LOST
    }
}
