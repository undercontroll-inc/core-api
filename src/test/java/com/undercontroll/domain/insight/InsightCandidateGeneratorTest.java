package com.undercontroll.domain.insight;

import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightMarketData;
import com.undercontroll.domain.model.insight.InsightMetric;
import com.undercontroll.domain.model.insight.InsightType;
import com.undercontroll.domain.model.insight.MetricUnit;
import com.undercontroll.domain.model.insight.RepairRelation;
import com.undercontroll.domain.model.market.MarketRisingProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.undercontroll.domain.insight.InsightMarketFixtures.BUCKET;
import static com.undercontroll.domain.insight.InsightMarketFixtures.data;
import static com.undercontroll.domain.insight.InsightMarketFixtures.rising;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsightCandidateGeneratorTest {

    @Test
    @DisplayName("builds one candidate per eligible row, with stable ids in type order")
    void candidateIds() {
        List<String> ids = InsightCandidateGenerator.generate(data()).stream().map(InsightCandidate::id).toList();

        assertEquals(List.of(
                "TENDENCIA_ALTA:MLB1",
                "TENDENCIA_ALTA:MLB2",
                "ALERTA_PRECO:MLB-MICROWAVES",
                "ALERTA_PRECO:MLB-AIR_FRYERS",
                "OPORTUNIDADE_ESTOQUE:MLB-AIR_FRYERS",
                "OPORTUNIDADE_REPARO:MLB-FANS",
                "DESTAQUE_MARCA:mondial",
                "DESTAQUE_MARCA:philco",
                "ALERTA_MERCADO:MLB5",
                "ALERTA_MERCADO:MLB3",
                "RECOMENDACAO:MLB-MICROWAVES",
                "RECOMENDACAO:MLB-AIR_FRYERS"
        ), ids);
    }

    @Test
    @DisplayName("rising products bind rank metrics to the product and keep the median only with three valid prices")
    void risingProducts() {
        List<InsightCandidate> candidates = InsightCandidateGenerator.generate(data());
        InsightCandidate exact = find(candidates, "TENDENCIA_ALTA:MLB1");
        InsightCandidate noRepair = find(candidates, "TENDENCIA_ALTA:MLB2");

        assertEquals("Micro-ondas Electrolux 20L", exact.subject());
        assertEquals("Micro-ondas", exact.categoryName());
        assertEquals(RepairRelation.NIVEL_1_EXATO, exact.repairRelation());
        assertEquals("HIGH", exact.confidence());
        assertEquals(BUCKET, exact.bucketKey());
        assertNull(exact.comparisonBucketKey());
        assertEquals(List.of(
                new InsightMetric("posicao", 1, MetricUnit.RANK),
                new InsightMetric("subida_posicoes", 2, MetricUnit.COUNT),
                new InsightMetric("destaque", 91.2, MetricUnit.SCORE),
                new InsightMetric("preco_mediano", 579.9, MetricUnit.CURRENCY)
        ), exact.metrics());
        assertTrue(noRepair.metric("preco_mediano").isEmpty());
        assertEquals(RepairRelation.NIVEL_3_CATEGORIA, noRepair.repairRelation());
    }

    @Test
    @DisplayName("price alerts come from reliable cohort movements and carry their comparison month")
    void priceAlerts() {
        List<InsightCandidate> candidates = InsightCandidateGenerator.generate(data());
        InsightCandidate microwave = find(candidates, "ALERTA_PRECO:MLB-MICROWAVES");
        InsightCandidate airFryer = find(candidates, "ALERTA_PRECO:MLB-AIR_FRYERS");

        assertEquals("2026-07", microwave.comparisonBucketKey());
        assertEquals("HIGH", microwave.confidence());
        assertEquals("LOW", airFryer.confidence());
        assertEquals(Map.of(
                "variacao_preco_pct", 10.9,
                "produtos_pareados", 6.0,
                "mediana_atual", 599.0,
                "mediana_anterior", 540.0
        ), microwave.evidenceFields());
        assertEquals("vw_market_price_movement", microwave.evidenceView());
        assertTrue(candidates.stream().noneMatch(candidate -> candidate.id().endsWith("MLB-BLENDERS")));
    }

    @Test
    @DisplayName("stock opportunities only cover falling prices in categories the workshop repairs")
    void stockOpportunities() {
        List<InsightCandidate> stock = ofType(InsightCandidateGenerator.generate(data()), InsightType.OPORTUNIDADE_ESTOQUE);

        assertEquals(1, stock.size());
        assertEquals(RepairRelation.NIVEL_3_CATEGORIA, stock.getFirst().repairRelation());
        assertEquals(-6.5, stock.getFirst().metric("variacao_preco_pct").orElseThrow().value());
    }

    @Test
    @DisplayName("repair opportunities, brands, dispersion and recommendations follow their eligibility rules")
    void otherTypes() {
        List<InsightCandidate> candidates = InsightCandidateGenerator.generate(data());

        InsightCandidate repair = find(candidates, "OPORTUNIDADE_REPARO:MLB-FANS");
        assertEquals(RepairRelation.SEM_MATCH, repair.repairRelation());
        assertEquals("HIGH", repair.confidence());
        assertEquals(3.0, repair.metric("produtos_em_alta").orElseThrow().value());

        InsightCandidate mondial = find(candidates, "DESTAQUE_MARCA:mondial");
        assertEquals(RepairRelation.NIVEL_2_MARCA_CATEGORIA, mondial.repairRelation());
        assertEquals("HIGH", mondial.confidence());
        assertEquals("LOW", find(candidates, "DESTAQUE_MARCA:philco").confidence());

        InsightCandidate spread = find(candidates, "ALERTA_MERCADO:MLB5");
        assertEquals(60.0, spread.metric("dispersao_pct").orElseThrow().value());
        assertEquals("HIGH", spread.confidence());
        assertEquals("HIGH", find(candidates, "ALERTA_MERCADO:MLB3").confidence());
        assertTrue(candidates.stream().noneMatch(candidate -> candidate.id().equals("ALERTA_MERCADO:MLB4")));

        InsightCandidate recommendation = find(candidates, "RECOMENDACAO:MLB-MICROWAVES");
        assertEquals(12.0, recommendation.metric("consertos").orElseThrow().value());
        assertEquals(RepairRelation.NIVEL_3_CATEGORIA, recommendation.repairRelation());
    }

    @Test
    @DisplayName("caps each type and returns the same candidates whatever the row order")
    void deterministicAndCapped() {
        List<MarketRisingProduct> many = new ArrayList<>(IntStream.rangeClosed(1, 6)
                .mapToObj(i -> rising("MLB" + i, "Produto " + i, "x", "x:" + i, "MLB-X", i, 1, 50.0 + i, 100.0, 5))
                .toList());
        InsightMarketData base = data();
        List<InsightCandidate> first = InsightCandidateGenerator.generate(withRising(base, many));
        Collections.reverse(many);
        List<InsightCandidate> second = InsightCandidateGenerator.generate(withRising(base, many));

        assertEquals(first, second);
        assertEquals(List.of("TENDENCIA_ALTA:MLB6", "TENDENCIA_ALTA:MLB5", "TENDENCIA_ALTA:MLB4"),
                ofType(first, InsightType.TENDENCIA_ALTA).stream().map(InsightCandidate::id).toList());
    }

    @Test
    @DisplayName("no market rows means no candidates")
    void empty() {
        assertTrue(InsightCandidateGenerator.generate(new InsightMarketData(BUCKET, null, null, null, null, null,
                null, null, null)).isEmpty());
    }

    private static InsightMarketData withRising(InsightMarketData data, List<MarketRisingProduct> rising) {
        return new InsightMarketData(data.bucketKey(), data.catalog(), data.categories(), data.priceMovements(),
                data.stockOpportunities(), rising, data.brandMomentum(), data.priceDispersion(),
                data.uncoveredCategories());
    }

    private static List<InsightCandidate> ofType(List<InsightCandidate> candidates, InsightType type) {
        return candidates.stream().filter(candidate -> candidate.type() == type).toList();
    }

    private static InsightCandidate find(List<InsightCandidate> candidates, String id) {
        return candidates.stream().filter(candidate -> candidate.id().equals(id)).findFirst().orElseThrow();
    }
}
