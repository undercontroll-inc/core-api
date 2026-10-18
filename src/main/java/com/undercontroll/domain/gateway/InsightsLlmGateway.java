package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.insight.InsightDraft;
import com.undercontroll.domain.model.insight.InsightDraftRequest;

public interface InsightsLlmGateway {

    InsightDraft draft(InsightDraftRequest request);
}
