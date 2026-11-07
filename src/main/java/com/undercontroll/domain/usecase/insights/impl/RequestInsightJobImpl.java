package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.exception.InsightsDisabledException;
import com.undercontroll.domain.exception.MarketSnapshotAwaitingException;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.usecase.insights.EnqueueInsightJobPort;
import com.undercontroll.domain.usecase.insights.RequestInsightJobPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestInsightJobImpl implements RequestInsightJobPort {

    private final EnqueueInsightJobPort enqueueInsightJobPort;

    @Override
    public InsightJob execute() {
        EnqueueInsightJobPort.Result result = enqueueInsightJobPort.execute(true);
        return switch (result.status()) {
            case DISABLED -> throw new InsightsDisabledException();
            case AWAITING_DATA -> throw new MarketSnapshotAwaitingException();
            case ENQUEUED -> result.job();
        };
    }
}
