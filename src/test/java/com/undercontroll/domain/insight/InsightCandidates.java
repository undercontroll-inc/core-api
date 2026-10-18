package com.undercontroll.domain.insight;

import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightMetric;
import com.undercontroll.domain.model.insight.InsightType;
import com.undercontroll.domain.model.insight.MetricUnit;
import com.undercontroll.domain.model.insight.RepairRelation;

import java.util.List;

public final class InsightCandidates {

    public static final InsightCandidate PRICE = new InsightCandidate(
            "ALERTA_PRECO:MLB-MICROWAVES", InsightType.ALERTA_PRECO, "Micro-ondas", "MLB-MICROWAVES", "Micro-ondas",
            "2026-08", "2026-07", "HIGH", RepairRelation.NIVEL_3_CATEGORIA, "vw_market_price_movement",
            List.of(
                    new InsightMetric("variacao_preco_pct", -6.5, MetricUnit.PERCENT),
                    new InsightMetric("produtos_pareados", 6, MetricUnit.COUNT),
                    new InsightMetric("mediana_atual", 1599.9, MetricUnit.CURRENCY)
            ));

    public static final InsightCandidate RISING = new InsightCandidate(
            "TENDENCIA_ALTA:MLB1", InsightType.TENDENCIA_ALTA, "Micro-ondas Electrolux 20L", "MLB-MICROWAVES",
            "Micro-ondas", "2026-08", null, "LOW", RepairRelation.NIVEL_1_EXATO, "vw_market_rising_products",
            List.of(
                    new InsightMetric("posicao", 1, MetricUnit.RANK),
                    new InsightMetric("destaque", 91.25, MetricUnit.SCORE)
            ));

    public static final InsightCandidate BRAND = new InsightCandidate(
            "DESTAQUE_MARCA:mondial", InsightType.DESTAQUE_MARCA, "Mondial", null, null,
            "2026-08", null, "HIGH", RepairRelation.SEM_MATCH, "vw_market_brand_summary",
            List.of(new InsightMetric("produtos_em_alta", 3, MetricUnit.COUNT)));

    private InsightCandidates() {
    }
}
