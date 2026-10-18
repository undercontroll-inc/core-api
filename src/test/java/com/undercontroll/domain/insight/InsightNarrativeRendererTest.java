package com.undercontroll.domain.insight;

import com.undercontroll.domain.model.insight.InsightDraft;
import com.undercontroll.domain.model.insight.InsightGenerationInput;
import com.undercontroll.domain.model.insight.InsightMetric;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.domain.model.insight.MetricUnit;
import com.undercontroll.domain.model.market.MatchCoverage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.undercontroll.domain.insight.InsightCandidates.BRAND;
import static com.undercontroll.domain.insight.InsightCandidates.PRICE;
import static com.undercontroll.domain.insight.InsightCandidates.RISING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InsightNarrativeRendererTest {

    @Test
    @DisplayName("placeholders become pt-BR values from the candidate itself")
    void rendersPlaceholders() {
        String text = InsightNarrativeRenderer.render(
                "No {{mes_atual}}, {{categoria}} caiu {{variacao_preco_pct}} frente ao {{mes_comparacao}} "
                        + "em {{produtos_pareados}} produtos, mediana de {{mediana_atual}}.", PRICE);

        assertEquals("No mês de agosto de 2026, Micro-ondas caiu 6,5% frente ao mês de julho de 2026 "
                + "em 6 produtos, mediana de R$ 1.599,90.", text);
        assertEquals("Micro-ondas Electrolux 20L na posição 1, destaque 91,3.",
                InsightNarrativeRenderer.render("{{assunto}} na posição {{posicao}}, destaque {{destaque}}.", RISING));
        assertNull(InsightNarrativeRenderer.render(null, RISING));
    }

    @Test
    @DisplayName("a placeholder the candidate does not have is an error, never a blank")
    void unknownPlaceholder() {
        assertThrows(IllegalArgumentException.class, () -> InsightNarrativeRenderer.render("{{mediana_atual}}", RISING));
    }

    @Test
    @DisplayName("formats every metric unit")
    void formats() {
        assertEquals("12,0%", InsightNarrativeRenderer.format(new InsightMetric("p", 12, MetricUnit.PERCENT)));
        assertEquals("R$ 99,90", InsightNarrativeRenderer.format(new InsightMetric("c", 99.9, MetricUnit.CURRENCY)));
        assertEquals("7", InsightNarrativeRenderer.format(new InsightMetric("n", 7.0, MetricUnit.COUNT)));
        assertEquals("3", InsightNarrativeRenderer.format(new InsightMetric("r", 3, MetricUnit.RANK)));
        assertEquals("80,0", InsightNarrativeRenderer.format(new InsightMetric("s", 80, MetricUnit.SCORE)));
    }

    @Test
    @DisplayName("the payload takes period, coverage, evidence and limitations from the server input")
    void rendersPayload() {
        InsightGenerationInput input = new InsightGenerationInput("2026-08", "2026-07", new MatchCoverage(1, 2, 3, 4),
                List.of(PRICE, RISING, BRAND), List.of(), List.of("limitação"));
        InsightDraft draft = new InsightDraft(List.of(
                new InsightDraft.Item(BRAND.id(), "{{assunto}} tem {{produtos_em_alta}} produtos em alta.", "Avaliar peças.")));

        InsightsPayload payload = InsightNarrativeRenderer.renderPayload(draft, input);

        assertEquals(new InsightsPayload.Periodo("2026-08", "2026-07"), payload.periodo());
        assertEquals(new InsightsPayload.CoberturaMatch(1, 2, 3, 4), payload.coberturaMatch());
        assertEquals(List.of("limitação"), payload.limitacoes());
        InsightsPayload.InsightItem item = payload.insights().getFirst();
        assertEquals(new InsightsPayload.InsightItem(BRAND.id(), "Mondial tem 3 produtos em alta.", "DESTAQUE_MARCA",
                null, null, new InsightsPayload.Evidencia("vw_market_brand_summary", Map.of("produtos_em_alta", 3.0)),
                "HIGH", "SEM_MATCH", "Avaliar peças."), item);
        assertThrows(IllegalArgumentException.class, () -> InsightNarrativeRenderer.renderPayload(
                new InsightDraft(List.of(new InsightDraft.Item("X", "t", "a"))), input));
    }
}
