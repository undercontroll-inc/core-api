package com.undercontroll.domain.insight;

import com.undercontroll.domain.market.MarketEvidence;
import com.undercontroll.domain.market.RepairCatalogMatcher;
import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightGenerationInput;
import com.undercontroll.domain.model.insight.InsightMarketData;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.domain.model.insight.RepairRelation;
import com.undercontroll.domain.model.market.MarketProductCurrent;
import com.undercontroll.domain.model.market.MatchCoverage;

import java.util.ArrayList;
import java.util.List;

public final class InsightInputs {

    static final int RISING_PRODUCTS_LIMIT = 10;
    static final String POPULARITY =
            "Os dados refletem popularidade e preço anunciado no Mercado Livre, não volume de vendas.";
    static final String NO_COMPARISON =
            "Não há mês anterior comparável com produtos suficientes; variações de preço não foram afirmadas.";
    static final String PARTIAL =
            "A coleta deste mês foi parcial; algumas categorias podem estar ausentes.";
    static final String NO_CATALOG =
            "Não há consertos registrados no período analisado; a relação com a oficina ficou indisponível.";
    static final String NO_CANDIDATES =
            "Os dados deste mês não sustentam conclusões além do panorama geral.";

    private InsightInputs() {
    }

    public static InsightGenerationInput build(
            InsightMarketData data,
            List<MarketProductCurrent> currentProducts,
            boolean partialSnapshot
    ) {
        List<InsightCandidate> candidates = InsightCandidateGenerator.generate(data);
        String comparison = MarketEvidence.comparisonBucket(data.priceMovements());
        MatchCoverage coverage = RepairCatalogMatcher.coverage(currentProducts, data.catalog());
        return new InsightGenerationInput(
                data.bucketKey(),
                comparison,
                coverage,
                candidates,
                risingProducts(data),
                limitations(comparison, partialSnapshot, data, candidates)
        );
    }

    static List<InsightsPayload.ProdutoEmAlta> risingProducts(InsightMarketData data) {
        RepairCatalogMatcher.CatalogIndex index = RepairCatalogMatcher.index(data.catalog());
        return data.risingProducts().stream()
                .limit(RISING_PRODUCTS_LIMIT)
                .map(product -> new InsightsPayload.ProdutoEmAlta(
                        product.title(),
                        product.brand(),
                        product.model(),
                        product.categoryName(),
                        product.rank(),
                        product.rankDelta(),
                        product.score(),
                        MarketEvidence.reliableMedian(product.validPriceCount()) ? product.priceMedian() : null,
                        product.validPriceCount(),
                        product.confidence(),
                        index.relation(product.productKey(), product.brandSlug(), product.domainId())
                                != RepairRelation.SEM_MATCH
                ))
                .toList();
    }

    static List<String> limitations(
            String comparison,
            boolean partialSnapshot,
            InsightMarketData data,
            List<InsightCandidate> candidates
    ) {
        List<String> limitations = new ArrayList<>();
        limitations.add(POPULARITY);
        if (comparison == null) {
            limitations.add(NO_COMPARISON);
        }
        if (partialSnapshot) {
            limitations.add(PARTIAL);
        }
        if (data.catalog().isEmpty()) {
            limitations.add(NO_CATALOG);
        }
        if (candidates.isEmpty()) {
            limitations.add(NO_CANDIDATES);
        }
        return limitations;
    }
}
