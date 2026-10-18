package com.undercontroll.domain.usecase.insights;

import com.undercontroll.domain.model.insight.InsightJob;

public interface EnqueueInsightJobPort {

    Result execute(boolean requeueFailed);

    record Result(Status status, InsightJob job) {

        public enum Status {
            ENQUEUED,
            AWAITING_DATA,
            DISABLED
        }

        public static Result awaitingData() {
            return new Result(Status.AWAITING_DATA, null);
        }

        public static Result disabled() {
            return new Result(Status.DISABLED, null);
        }
    }
}
