package com.undercontroll.domain.model;

import com.undercontroll.domain.enums.InsightGenerationStatus;
import com.undercontroll.domain.model.insight.InsightsPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketMonthlyInsight {

    private Integer id;
    private String bucketKey;
    private String comparisonBucketKey;
    private InsightGenerationStatus status;
    private InsightsPayload payload;
    private String provider;
    private String model;
    private String promptVersion;
    private String errorMessage;
    private LocalDateTime generatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
