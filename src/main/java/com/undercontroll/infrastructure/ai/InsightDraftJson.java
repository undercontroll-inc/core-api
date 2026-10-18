package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.undercontroll.domain.model.insight.InsightDraft;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InsightDraftJson(@JsonProperty("insights") List<Item> insights) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("candidate_id") String candidateId,
            @JsonProperty("text") String text,
            @JsonProperty("action") String action
    ) {
    }

    public static InsightDraftJson from(InsightDraft draft) {
        return new InsightDraftJson(draft.insights().stream()
                .map(item -> new Item(item.candidateId(), item.text(), item.action()))
                .toList());
    }

    public InsightDraft toDomain() {
        return new InsightDraft(insights == null ? List.of() : insights.stream()
                .map(item -> item == null ? null : new InsightDraft.Item(item.candidateId(), item.text(), item.action()))
                .toList());
    }
}
