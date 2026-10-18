package com.undercontroll.domain.insight;

import com.undercontroll.domain.market.MarketEvidence;
import com.undercontroll.domain.market.RepairCatalogMatcher;
import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightMarketData;
import com.undercontroll.domain.model.insight.InsightMetric;
import com.undercontroll.domain.model.insight.InsightType;
import com.undercontroll.domain.model.insight.MetricUnit;
import com.undercontroll.domain.model.insight.RepairRelation;
import com.undercontroll.domain.model.market.MarketBrandSummary;
import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.model.market.MarketPriceMovement;
import com.undercontroll.domain.model.market.MarketProductCurrent;
import com.undercontroll.domain.model.market.MarketRisingProduct;
import com.undercontroll.domain.model.market.RepairCatalogItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class InsightCandidateGenerator {

    public static final String HIGH = "HIGH";
    public static final String LOW = "LOW";

    static final int RISING_LIMIT = 3;
    static final int PRICE_LIMIT = 3;
    static final int STOCK_LIMIT = 2;
    static final int REPAIR_LIMIT = 2;
    static final int BRAND_LIMIT = 2;
    static final int DISPERSION_LIMIT = 2;
    static final int RECOMMENDATION_LIMIT = 2;
    static final double MIN_DISPERSION_PCT = 25.0;
    static final int HIGH_CONFIDENCE_PAIRED = 5;
    static final int HIGH_CONFIDENCE_PRICES = 10;

    private static final String VIEW_RISING = "vw_market_rising_products";
    private static final String VIEW_MOVEMENT = "vw_market_price_movement";
    private static final String VIEW_CATEGORY = "vw_market_category_summary";
    private static final String VIEW_BRAND = "vw_market_brand_summary";
    private static final String VIEW_PRODUCT = "vw_market_product_current";

    private InsightCandidateGenerator() {
    }

    public static List<InsightCandidate> generate(InsightMarketData data) {
        Context context = new Context(data);
        return Stream.of(
                        rising(context),
                        priceAlerts(context),
                        stockOpportunities(context),
                        repairOpportunities(context),
                        brands(context),
                        dispersion(context),
                        recommendations(context))
                .flatMap(List::stream)
                .toList();
    }

    private static List<InsightCandidate> rising(Context context) {
        return context.data.risingProducts().stream()
                .filter(product -> product.externalId() != null && product.rank() != null)
                .sorted(Comparator.comparing(MarketRisingProduct::score, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MarketRisingProduct::externalId))
                .limit(RISING_LIMIT)
                .map(product -> {
                    List<InsightMetric> metrics = new ArrayList<>();
                    metrics.add(new InsightMetric("posicao", product.rank(), MetricUnit.RANK));
                    if (product.rankDelta() != null && product.rankDelta() > 0) {
                        metrics.add(new InsightMetric("subida_posicoes", product.rankDelta(), MetricUnit.COUNT));
                    }
                    if (product.score() != null) {
                        metrics.add(new InsightMetric("destaque", product.score(), MetricUnit.SCORE));
                    }
                    if (product.priceMedian() != null && MarketEvidence.reliableMedian(product.validPriceCount())) {
                        metrics.add(new InsightMetric("preco_mediano", product.priceMedian(), MetricUnit.CURRENCY));
                    }
                    return new InsightCandidate(
                            id(InsightType.TENDENCIA_ALTA, product.externalId()),
                            InsightType.TENDENCIA_ALTA,
                            product.title(),
                            product.domainId(),
                            context.categoryName(product.domainId(), product.categoryName()),
                            context.bucketKey(),
                            null,
                            HIGH.equals(product.confidence()) ? HIGH : LOW,
                            context.index.relation(product.productKey(), product.brandSlug(), product.domainId()),
                            VIEW_RISING,
                            metrics
                    );
                })
                .toList();
    }

    private static List<InsightCandidate> priceAlerts(Context context) {
        return context.data.priceMovements().stream()
                .filter(MarketEvidence::reliableMovement)
                .sorted(Comparator.comparing((MarketPriceMovement row) -> Math.abs(row.priceIndexDeltaPct())).reversed()
                        .thenComparing(MarketPriceMovement::domainId))
                .limit(PRICE_LIMIT)
                .map(row -> movement(context, InsightType.ALERTA_PRECO, row))
                .toList();
    }

    private static List<InsightCandidate> stockOpportunities(Context context) {
        return context.data.stockOpportunities().stream()
                .filter(MarketEvidence::reliableMovement)
                .filter(row -> row.priceIndexDeltaPct() < 0)
                .filter(row -> context.index.relation(null, null, row.domainId()) != RepairRelation.SEM_MATCH)
                .sorted(Comparator.comparing(MarketPriceMovement::priceIndexDeltaPct)
                        .thenComparing(MarketPriceMovement::domainId))
                .limit(STOCK_LIMIT)
                .map(row -> movement(context, InsightType.OPORTUNIDADE_ESTOQUE, row))
                .toList();
    }

    private static InsightCandidate movement(Context context, InsightType type, MarketPriceMovement row) {
        List<InsightMetric> metrics = new ArrayList<>();
        metrics.add(new InsightMetric("variacao_preco_pct", row.priceIndexDeltaPct(), MetricUnit.PERCENT));
        metrics.add(new InsightMetric("produtos_pareados", row.pairedCount(), MetricUnit.COUNT));
        if (row.currentCohortMedian() != null) {
            metrics.add(new InsightMetric("mediana_atual", row.currentCohortMedian(), MetricUnit.CURRENCY));
        }
        if (row.previousCohortMedian() != null) {
            metrics.add(new InsightMetric("mediana_anterior", row.previousCohortMedian(), MetricUnit.CURRENCY));
        }
        String category = context.categoryName(row.domainId(), null);
        return new InsightCandidate(
                id(type, row.domainId()),
                type,
                category,
                row.domainId(),
                category,
                context.bucketKey(),
                row.previousBucketKey(),
                row.pairedCount() >= HIGH_CONFIDENCE_PAIRED ? HIGH : LOW,
                context.index.relation(null, null, row.domainId()),
                VIEW_MOVEMENT,
                metrics
        );
    }

    private static List<InsightCandidate> repairOpportunities(Context context) {
        return context.data.uncoveredCategories().stream()
                .filter(row -> row.domainId() != null && positive(row.risingCount()))
                .filter(row -> context.index.relation(null, null, row.domainId()) == RepairRelation.SEM_MATCH)
                .sorted(Comparator.comparing(MarketCategorySummary::risingCount, Comparator.reverseOrder())
                        .thenComparing(MarketCategorySummary::domainId))
                .limit(REPAIR_LIMIT)
                .map(row -> category(context, InsightType.OPORTUNIDADE_REPARO, row, List.of()))
                .toList();
    }

    private static List<InsightCandidate> recommendations(Context context) {
        Map<String, Long> volumeByDomain = context.data.catalog().stream()
                .filter(item -> item.domainId() != null)
                .collect(Collectors.groupingBy(RepairCatalogItem::domainId, LinkedHashMap::new,
                        Collectors.summingLong(RepairCatalogItem::volume)));
        return context.data.categories().stream()
                .filter(row -> row.domainId() != null && volumeByDomain.containsKey(row.domainId()))
                .sorted(Comparator.comparing((MarketCategorySummary row) -> volumeByDomain.get(row.domainId()))
                        .reversed()
                        .thenComparing(MarketCategorySummary::domainId))
                .limit(RECOMMENDATION_LIMIT)
                .map(row -> category(context, InsightType.RECOMENDACAO, row, List.of(
                        new InsightMetric("consertos", volumeByDomain.get(row.domainId()), MetricUnit.COUNT))))
                .toList();
    }

    private static InsightCandidate category(
            Context context,
            InsightType type,
            MarketCategorySummary row,
            List<InsightMetric> extra
    ) {
        List<InsightMetric> metrics = new ArrayList<>(extra);
        if (row.productCount() != null) {
            metrics.add(new InsightMetric("produtos", row.productCount(), MetricUnit.COUNT));
        }
        if (row.risingCount() != null) {
            metrics.add(new InsightMetric("produtos_em_alta", row.risingCount(), MetricUnit.COUNT));
        }
        if (row.avgScore() != null) {
            metrics.add(new InsightMetric("destaque_medio", row.avgScore(), MetricUnit.SCORE));
        }
        String category = context.categoryName(row.domainId(), row.categoryName());
        return new InsightCandidate(
                id(type, row.domainId()),
                type,
                category,
                row.domainId(),
                category,
                context.bucketKey(),
                null,
                positive(row.highConfidenceCount()) ? HIGH : LOW,
                context.index.relation(null, null, row.domainId()),
                VIEW_CATEGORY,
                metrics
        );
    }

    private static List<InsightCandidate> brands(Context context) {
        return context.data.brandMomentum().stream()
                .filter(row -> row.brandSlug() != null && positive(row.risingCount()))
                .sorted(Comparator.comparing(MarketBrandSummary::risingCount, Comparator.reverseOrder())
                        .thenComparing(MarketBrandSummary::productCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MarketBrandSummary::brandSlug))
                .limit(BRAND_LIMIT)
                .map(row -> {
                    List<InsightMetric> metrics = new ArrayList<>();
                    metrics.add(new InsightMetric("produtos_em_alta", row.risingCount(), MetricUnit.COUNT));
                    if (row.productCount() != null) {
                        metrics.add(new InsightMetric("produtos", row.productCount(), MetricUnit.COUNT));
                    }
                    if (row.bestRank() != null) {
                        metrics.add(new InsightMetric("melhor_posicao", row.bestRank(), MetricUnit.RANK));
                    }
                    return new InsightCandidate(
                            id(InsightType.DESTAQUE_MARCA, row.brandSlug()),
                            InsightType.DESTAQUE_MARCA,
                            row.brandName() == null ? row.brandSlug() : row.brandName(),
                            null,
                            null,
                            context.bucketKey(),
                            null,
                            row.risingCount() >= 2 ? HIGH : LOW,
                            context.index.brandRelation(row.brandSlug()),
                            VIEW_BRAND,
                            metrics
                    );
                })
                .toList();
    }

    private static List<InsightCandidate> dispersion(Context context) {
        record Spread(MarketProductCurrent product, double pct) {
        }
        return context.data.priceDispersion().stream()
                .filter(product -> product.externalId() != null && MarketEvidence.reliableMedian(product.validPriceCount()))
                .filter(product -> product.priceMin() != null && product.priceMax() != null && product.priceMin() > 0)
                .map(product -> new Spread(product, round((product.priceMax() - product.priceMin()) * 100.0 / product.priceMin())))
                .filter(spread -> spread.pct() >= MIN_DISPERSION_PCT)
                .sorted(Comparator.comparing(Spread::pct).reversed()
                        .thenComparing(spread -> spread.product().externalId()))
                .limit(DISPERSION_LIMIT)
                .map(spread -> {
                    MarketProductCurrent product = spread.product();
                    boolean high = Boolean.TRUE.equals(product.sampleComplete())
                            || product.validPriceCount() >= HIGH_CONFIDENCE_PRICES;
                    return new InsightCandidate(
                            id(InsightType.ALERTA_MERCADO, product.externalId()),
                            InsightType.ALERTA_MERCADO,
                            product.title(),
                            product.domainId(),
                            context.categoryName(product.domainId(), product.categoryName()),
                            context.bucketKey(),
                            null,
                            high ? HIGH : LOW,
                            context.index.relation(product.productKey(), product.brandSlug(), product.domainId()),
                            VIEW_PRODUCT,
                            List.of(
                                    new InsightMetric("preco_minimo", product.priceMin(), MetricUnit.CURRENCY),
                                    new InsightMetric("preco_maximo", product.priceMax(), MetricUnit.CURRENCY),
                                    new InsightMetric("dispersao_pct", spread.pct(), MetricUnit.PERCENT),
                                    new InsightMetric("precos_validos", product.validPriceCount(), MetricUnit.COUNT)
                            )
                    );
                })
                .toList();
    }

    private static String id(InsightType type, String key) {
        return type.name() + ":" + key;
    }

    private static boolean positive(Long value) {
        return value != null && value > 0;
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static final class Context {

        private final InsightMarketData data;
        private final RepairCatalogMatcher.CatalogIndex index;
        private final Map<String, String> categoryNames;

        private Context(InsightMarketData data) {
            this.data = data;
            this.index = RepairCatalogMatcher.index(data.catalog());
            this.categoryNames = data.categories().stream()
                    .filter(row -> row.domainId() != null && row.categoryName() != null)
                    .collect(Collectors.toMap(MarketCategorySummary::domainId, MarketCategorySummary::categoryName,
                            (first, second) -> first));
        }

        String bucketKey() {
            return data.bucketKey();
        }

        String categoryName(String domainId, String fallback) {
            return categoryNames.getOrDefault(domainId, fallback);
        }
    }
}
