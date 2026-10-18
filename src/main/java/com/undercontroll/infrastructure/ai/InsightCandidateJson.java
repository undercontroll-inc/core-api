package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.undercontroll.domain.insight.InsightContract;
import com.undercontroll.domain.insight.InsightNarrativeRenderer;
import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightMetric;

import java.util.LinkedHashMap;
import java.util.Map;

public record InsightCandidateJson(
        @JsonProperty("candidate_id") String candidateId,
        @JsonProperty("tipo") String tipo,
        @JsonProperty("confianca") String confianca,
        @JsonProperty("relacao_com_consertos") String relacaoComConsertos,
        @JsonProperty("placeholders") Map<String, String> placeholders,
        @JsonProperty("valores") Map<String, Double> valores
) {

    public static InsightCandidateJson from(InsightCandidate candidate) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        InsightContract.placeholders(candidate)
                .forEach(name -> placeholders.put(name, InsightNarrativeRenderer.render("{{" + name + "}}", candidate)));
        Map<String, Double> values = new LinkedHashMap<>();
        for (InsightMetric metric : candidate.metrics()) {
            values.put(metric.name(), metric.value());
        }
        return new InsightCandidateJson(
                candidate.id(),
                candidate.type().name(),
                candidate.confidence(),
                candidate.repairRelation().name(),
                placeholders,
                values
        );
    }
}
