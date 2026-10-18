package com.undercontroll.domain.model.insight;

public record InsightGenerationSettings(
        String provider,
        String model,
        String promptVersion,
        int maxInsights,
        int repairCatalogDays,
        String modelConfigHash
) {
}
