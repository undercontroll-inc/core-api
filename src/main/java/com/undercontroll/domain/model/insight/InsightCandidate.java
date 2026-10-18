package com.undercontroll.domain.model.insight;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record InsightCandidate(
        String id,
        InsightType type,
        String subject,
        String domainId,
        String categoryName,
        String bucketKey,
        String comparisonBucketKey,
        String confidence,
        RepairRelation repairRelation,
        String evidenceView,
        List<InsightMetric> metrics
) {

    public InsightCandidate {
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }

    public Optional<InsightMetric> metric(String name) {
        return metrics.stream().filter(metric -> metric.name().equals(name)).findFirst();
    }

    public Map<String, Object> evidenceFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        metrics.forEach(metric -> fields.put(metric.name(), metric.value()));
        return fields;
    }
}
