package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightMetric;
import com.undercontroll.domain.model.insight.InsightPromptContext;
import com.undercontroll.domain.model.insight.InsightType;
import com.undercontroll.domain.model.insight.MetricUnit;
import com.undercontroll.domain.model.insight.RepairRelation;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.domain.model.market.MatchCoverage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InsightPayloadValidatorTest {

    private final InsightPayloadValidator validator = new InsightPayloadValidator(7);
    private static final InsightCandidate PRICE = new InsightCandidate(
            "ALERTA_PRECO:MLB-MICROWAVES", InsightType.ALERTA_PRECO, "Micro-ondas", "MLB-MICROWAVES", "Micro-ondas",
            "2026-08", "2026-07", "HIGH", RepairRelation.NIVEL_3_CATEGORIA, "vw_market_price_movement",
            List.of(new InsightMetric("variacao_preco_pct", 10.9, MetricUnit.PERCENT)));
    private static final InsightCandidate BRAND = new InsightCandidate(
            "DESTAQUE_MARCA:mondial", InsightType.DESTAQUE_MARCA, "Mondial", null, null,
            "2026-08", null, "LOW", RepairRelation.SEM_MATCH, "vw_market_brand_summary",
            List.of(new InsightMetric("produtos_em_alta", 2, MetricUnit.COUNT)));
    private final InsightPromptContext context =
            new InsightPromptContext("2026-08", "2026-07", MatchCoverage.empty(), List.of(PRICE, BRAND));

    @Test
    @DisplayName("accepts insights that reference known candidates with the same type")
    void acceptsTraceablePayload() {

        assertDoesNotThrow(() -> validator.validate(validPayload(), context));
    }

    @Test
    @DisplayName("rejects unknown candidates, mismatched types and sales language")
    void rejectsHallucinationAndSalesLanguage() {
        InsightsPayload unknown = withItem(new InsightsPayload.InsightItem("ALERTA_PRECO:MLB-FANS", pad("ok"),
                "ALERTA_PRECO", null, null, null, null, null, "Revisar."));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(unknown, context));

        InsightsPayload mismatched = withItem(new InsightsPayload.InsightItem(BRAND.id(), pad("ok"),
                "ALERTA_PRECO", null, null, null, null, null, "Revisar."));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(mismatched, context));

        InsightsPayload sales = withText(pad("A marca vendeu 1000 unidades no mês"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(sales, context));

        InsightsPayload tooMany = new InsightsPayload(
                new InsightsPayload.Periodo("2026-08", "2026-07"),
                InsightsPayload.CoberturaMatch.from(MatchCoverage.empty()),
                Stream.generate(() -> insight("ALERTA_PRECO", pad("ok"), Map.of("price_delta_pct", 11.11)))
                        .limit(8)
                        .toList(),
                List.of(),
                List.of("lim")
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(tooMany, context));

        InsightsPayload badType = new InsightsPayload(
                new InsightsPayload.Periodo("2026-08", "2026-07"),
                InsightsPayload.CoberturaMatch.from(MatchCoverage.empty()),
                List.of(insight("INVALID", pad("ok"), Map.of("price_delta_pct", 11.11))),
                List.of(),
                List.of("lim")
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(badType, context));

        InsightsPayload legacyType = new InsightsPayload(
                new InsightsPayload.Periodo("2026-08", "2026-07"),
                InsightsPayload.CoberturaMatch.from(MatchCoverage.empty()),
                List.of(insight("PRECO_ALTA", pad("ok"), Map.of("price_delta_pct", 11.11))),
                List.of(),
                List.of("lim")
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(legacyType, context));

        assertThrows(IllegalArgumentException.class, () -> validator.validate(null, context));
    }

    @Test
    @DisplayName("rejects duplicate candidates and out-of-range text")
    void rejectsDuplicateTypesAndTextLength() {

        InsightsPayload duplicate = new InsightsPayload(
                new InsightsPayload.Periodo("2026-08", "2026-07"),
                InsightsPayload.CoberturaMatch.from(MatchCoverage.empty()),
                List.of(
                        insight("ALERTA_PRECO", pad("primeiro"), Map.of("price_delta_pct", 11.11)),
                        insight("ALERTA_PRECO", pad("segundo"), Map.of("price_delta_pct", 11.11))
                ),
                List.of(),
                List.of("lim")
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(duplicate, context));

        InsightsPayload shortText = withText("texto curto");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(shortText, context));

        InsightsPayload longText = withText("x".repeat(1201));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(longText, context));

        InsightsPayload longAction = new InsightsPayload(
                new InsightsPayload.Periodo("2026-08", "2026-07"),
                InsightsPayload.CoberturaMatch.from(MatchCoverage.empty()),
                List.of(new InsightsPayload.InsightItem(
                        PRICE.id(),
                        pad("ok"),
                        "ALERTA_PRECO",
                        "MLB-MICROWAVES",
                        "Micro-ondas",
                        new InsightsPayload.Evidencia("vw_market_price_movement", Map.of("price_delta_pct", 11.11)),
                        "HIGH",
                        "NIVEL_3_CATEGORIA",
                        "a".repeat(281)
                )),
                List.of(),
                List.of("lim")
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(longAction, context));
    }

    @Test
    @DisplayName("rejects internal jargon such as bucket and ISO month codes in visible copy")
    void rejectsTechnicalJargon() {

        InsightsPayload bucketInText = withText(pad("No bucket atual os micro-ondas subiram de preço"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(bucketInText, context));

        InsightsPayload isoMonth = withText(pad("Em 2026-08 os micro-ondas subiram de preço no Mercado Livre"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(isoMonth, context));

        InsightsPayload bucketInAction = new InsightsPayload(
                new InsightsPayload.Periodo("2026-08", "2026-07"),
                InsightsPayload.CoberturaMatch.from(MatchCoverage.empty()),
                List.of(new InsightsPayload.InsightItem(
                        PRICE.id(),
                        pad("Micro-ondas subiram de preço neste mês"),
                        "ALERTA_PRECO",
                        "MLB-MICROWAVES",
                        "Micro-ondas",
                        new InsightsPayload.Evidencia("vw_market_price_movement", Map.of("price_delta_pct", 11.11)),
                        "HIGH",
                        "NIVEL_3_CATEGORIA",
                        "Revisar peça no bucket 2026-08."
                )),
                List.of(),
                List.of("lim")
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(bucketInAction, context));
    }

    @Test
    @DisplayName("rejects empty limitations and wrong coverage")
    void rejectsMissingLimitationsAndCoverage() {
        InsightPromptContext context = new InsightPromptContext("2026-08", "2026-07", new MatchCoverage(1, 0, 0, 0), List.of());
        InsightsPayload payload = new InsightsPayload(
                new InsightsPayload.Periodo("2026-08", "2026-07"),
                InsightsPayload.CoberturaMatch.from(MatchCoverage.empty()),
                List.of(),
                List.of(),
                List.of()
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(payload, context));
    }

    private InsightsPayload validPayload() {
        return new InsightsPayload(
                new InsightsPayload.Periodo("2026-08", "2026-07"),
                InsightsPayload.CoberturaMatch.from(MatchCoverage.empty()),
                List.of(insight("ALERTA_PRECO", pad("Micro-ondas subiram de preço"), Map.of(
                        "price_delta_pct", 11.11, "avg_price_median", 599.0, "product_count", 2))),
                List.of(),
                List.of("Os dados refletem popularidade e preço anunciado, não volume de vendas.")
        );
    }

    private InsightsPayload withItem(InsightsPayload.InsightItem item) {
        return new InsightsPayload(
                new InsightsPayload.Periodo("2026-08", "2026-07"),
                InsightsPayload.CoberturaMatch.from(MatchCoverage.empty()),
                List.of(item),
                List.of(),
                List.of("Os dados refletem popularidade e preço anunciado, não volume de vendas.")
        );
    }

    private InsightsPayload withText(String texto) {
        return new InsightsPayload(
                new InsightsPayload.Periodo("2026-08", "2026-07"),
                InsightsPayload.CoberturaMatch.from(MatchCoverage.empty()),
                List.of(insight("ALERTA_PRECO", texto, Map.of("price_delta_pct", 11.11))),
                List.of(),
                List.of("Os dados refletem popularidade e preço anunciado, não volume de vendas.")
        );
    }

    private InsightsPayload.InsightItem insight(String tipo, String texto, Map<String, Object> campos) {
        return new InsightsPayload.InsightItem(
                PRICE.id(),
                texto,
                tipo,
                "MLB-MICROWAVES",
                "Micro-ondas",
                new InsightsPayload.Evidencia("vw_market_price_movement", campos),
                "HIGH",
                "NIVEL_3_CATEGORIA",
                "Revisar o preço do conserto."
        );
    }

    private static String pad(String seed) {
        StringBuilder text = new StringBuilder(seed);
        String filler = " A assistência já conserta essa linha e o dado de popularidade reforça estoque de peça.";
        while (text.length() < 280) {
            text.append(filler);
        }
        return text.toString();
    }
}
