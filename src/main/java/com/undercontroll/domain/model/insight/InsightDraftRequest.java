package com.undercontroll.domain.model.insight;

import com.undercontroll.domain.insight.InsightContract;

import java.util.List;

public record InsightDraftRequest(
        InsightGenerationInput input,
        InsightContract contract,
        InsightDraft previousDraft,
        List<String> errors
) {
    public InsightDraftRequest {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static InsightDraftRequest first(InsightGenerationInput input, InsightContract contract) {
        return new InsightDraftRequest(input, contract, null, List.of());
    }

    public static InsightDraftRequest repair(
            InsightGenerationInput input,
            InsightContract contract,
            InsightDraft previousDraft,
            List<String> errors
    ) {
        return new InsightDraftRequest(input, contract, previousDraft, errors);
    }

    public boolean repairing() {
        return previousDraft != null;
    }
}
