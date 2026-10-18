package com.undercontroll.domain.usecase.insights;

import com.undercontroll.domain.model.insight.InsightsPayload;

public interface GetMarketInsightsPort {

    Output execute();

    record Output(InsightsPayload payload, String generatedAt) {

        public static Output empty() {
            return new Output(null, null);
        }
    }
}
