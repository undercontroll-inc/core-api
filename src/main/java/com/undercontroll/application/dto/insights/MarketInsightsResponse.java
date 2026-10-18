package com.undercontroll.application.dto.insights;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Insights of the published market snapshot with an explicit state. "
        + "Payload fields belong to availableBucketKey, which differs from requestedBucketKey when the content is stale.")
public record MarketInsightsResponse(
        @Schema(description = "READY: insights of the published snapshot. STALE: only an older publication exists and "
                + "no generation is running. AWAITING_DATA: the ETL has not published a snapshot yet (no payload). "
                + "GENERATING: the job for the published snapshot is pending or running. FAILED: the job failed; "
                + "see failureReason", allowableValues = {"READY", "STALE", "AWAITING_DATA", "GENERATING", "FAILED"})
        String state,
        @Schema(description = "Month (YYYY-MM) of the published market snapshot; null in AWAITING_DATA")
        String requestedBucketKey,
        @Schema(description = "Readable label of requestedBucketKey, e.g. mês de agosto de 2026")
        String requestedMonth,
        @Schema(description = "When the ETL published the snapshot (ISO-8601 UTC)")
        String snapshotPublishedAt,
        @Schema(description = "Month of the insights served below; null when nothing was ever published")
        String availableBucketKey,
        @Schema(description = "Readable label of availableBucketKey")
        String availableMonth,
        @Schema(description = "Month used for price comparisons; null without a comparable previous month")
        String comparisonBucketKey,
        @Schema(description = "When the served insights were published (ISO-8601 UTC)")
        String generatedAt,
        @Schema(description = "Error kind when state is FAILED: AUTH, CONFIG, RATE_LIMITED, TIMEOUT, TRANSIENT, "
                + "INVALID_OUTPUT, MARKET_DATA, PUBLISH, SUPERSEDED, UNKNOWN or INSIGHTS_DISABLED")
        String failureReason,
        MatchCoverageResponse matchCoverage,
        List<InsightItemResponse> insights,
        List<RisingProductResponse> risingProducts,
        List<String> limitations
) {

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
            @Schema(description = "Stable id of the server-computed candidate behind this insight")
            String id,
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
            @Schema(description = "Metrics of the candidate, by name, exactly as the server computed them")
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
            @Schema(description = "Median listed price; null when fewer than three valid prices were observed")
            Double medianPrice,
            @Schema(description = "Number of observed offers with a valid price behind the median")
            Integer validPriceCount,
            String confidence,
            Boolean servedByClient
    ) {
    }
}
