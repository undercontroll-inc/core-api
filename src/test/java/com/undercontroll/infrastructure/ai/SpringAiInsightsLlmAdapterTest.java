package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightMetric;
import com.undercontroll.domain.model.insight.InsightPromptContext;
import com.undercontroll.domain.model.insight.InsightType;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.domain.model.insight.MetricUnit;
import com.undercontroll.domain.model.insight.RepairRelation;
import com.undercontroll.domain.model.market.MatchCoverage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringAiInsightsLlmAdapterTest {

    @Test
    @DisplayName("category, evidence, confidence and repair relation come from the referenced candidate")
    void bindsCandidateFacts() {
        InsightCandidate candidate = new InsightCandidate("ALERTA_PRECO:MLB-MICROWAVES", InsightType.ALERTA_PRECO,
                "Micro-ondas", "MLB-MICROWAVES", "Micro-ondas", "2026-08", "2026-07", "HIGH",
                RepairRelation.NIVEL_3_CATEGORIA, "vw_market_price_movement",
                List.of(new InsightMetric("variacao_preco_pct", 10.9, MetricUnit.PERCENT)));
        InsightsPayload generated = new InsightsPayload(
                new InsightsPayload.Periodo("2026-08", "2026-07"),
                InsightsPayload.CoberturaMatch.from(MatchCoverage.empty()),
                List.of(new InsightsPayload.InsightItem(candidate.id(), "texto", "ALERTA_PRECO", "MLB-FANS",
                        "Ventilador", new InsightsPayload.Evidencia("vw_x", Map.of("price", 1)), "LOW", "SEM_MATCH",
                        "acao")),
                List.of(),
                List.of("lim")
        );

        InsightsPayload.InsightItem bound = SpringAiInsightsLlmAdapter.bind(generated,
                new InsightPromptContext("2026-08", "2026-07", MatchCoverage.empty(), List.of(candidate))).insights().getFirst();

        assertEquals("MLB-MICROWAVES", bound.categoria());
        assertEquals("Micro-ondas", bound.categoriaNome());
        assertEquals(new InsightsPayload.Evidencia("vw_market_price_movement", Map.of("variacao_preco_pct", 10.9)),
                bound.evidencia());
        assertEquals("HIGH", bound.confianca());
        assertEquals("NIVEL_3_CATEGORIA", bound.relacaoComConsertos());
        assertEquals("texto", bound.texto());
        assertEquals("acao", bound.acaoSugerida());
    }
}
