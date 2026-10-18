package com.undercontroll.application.dto.insights;

import java.util.List;
import java.util.Map;

public record MarketInsightsResponse(
        String bucketKey,
        String comparisonBucketKey,
        MatchCoverageResponse matchCoverage,
        List<InsightItemResponse> insights,
        List<RisingProductResponse> risingProducts,
        List<String> limitations,
        String generatedAt
) {
    public static MarketInsightsResponse empty() {
        return new MarketInsightsResponse(
                null,
                null,
                MatchCoverageResponse.empty(),
                List.of(),
                List.of(),
                List.of(),
                null
        );
    }

    public record MatchCoverageResponse(
            int exact,
            int brandCategory,
            int category,
            int none
    ) {
        public static MatchCoverageResponse empty() {
            return new MatchCoverageResponse(0, 0, 0, 0);
        }
    }

    public record InsightItemResponse(
            String type,
            String text,
            String category,
            String categoryName,
            EvidenceResponse evidence,
            String confidence,
            String repairRelation,
            String suggestedAction
    ) {
    }

    public record EvidenceResponse(
            String view,
            Map<String, Object> fields
    ) {
    }

    public record RisingProductResponse(
            String name,
            String brand,
            String model,
            String category,
            Integer rank,
            Integer positionDelta,
            Double score,
            Double medianPrice,
            Integer validPriceCount,
            String confidence,
            Boolean servedByClient
    ) {
    }
}
