package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightMonthLabel;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InsightCandidateJson(
        @JsonProperty("candidate_id") String candidateId,
        @JsonProperty("tipo") String tipo,
        @JsonProperty("assunto") String assunto,
        @JsonProperty("categoria") String categoria,
        @JsonProperty("mes_comparacao") String mesComparacao,
        @JsonProperty("confianca") String confianca,
        @JsonProperty("relacao_com_consertos") String relacaoComConsertos,
        @JsonProperty("metricas") Map<String, Object> metricas
) {

    public static InsightCandidateJson from(InsightCandidate candidate) {
        return new InsightCandidateJson(
                candidate.id(),
                candidate.type().name(),
                candidate.subject(),
                candidate.categoryName(),
                candidate.comparisonBucketKey() == null ? null : InsightMonthLabel.of(candidate.comparisonBucketKey()),
                candidate.confidence(),
                candidate.repairRelation().name(),
                candidate.evidenceFields()
        );
    }
}
