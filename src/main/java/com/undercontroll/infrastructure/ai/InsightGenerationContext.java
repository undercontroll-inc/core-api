package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.model.insight.InsightPromptContext;
import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.model.market.MatchCoverage;
import com.undercontroll.domain.model.market.RepairCatalogItem;

import java.util.List;
import java.util.Set;

public final class InsightGenerationContext {

    private InsightGenerationContext() {
    }

    public record State(
            String bucketKey,
            String comparisonBucketKey,
            MatchCoverage coverage,
            Set<String> clientDomainIds,
            List<RepairCatalogItem> catalog,
            List<MarketCategorySummary> categories,
            EvidenceIndex evidence
    ) {
        public static State from(InsightPromptContext context, EvidenceIndex evidence) {
            return new State(
                    context.bucketKey(),
                    context.comparisonBucketKey(),
                    context.coverage(),
                    context.clientDomainIds() == null ? Set.of() : context.clientDomainIds(),
                    context.catalog() == null ? List.of() : context.catalog(),
                    context.categories() == null ? List.of() : context.categories(),
                    evidence
            );
        }
    }
}
