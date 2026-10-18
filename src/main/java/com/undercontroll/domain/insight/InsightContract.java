package com.undercontroll.domain.insight;

import com.undercontroll.domain.model.insight.InsightCandidate;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

public record InsightContract(int maxInsights) {

    public static final String VERSION = "insight-draft.v1";
    public static final int MAX_INSIGHTS = 7;
    public static final int MIN_TEXT_LENGTH = 60;
    public static final int MAX_TEXT_LENGTH = 700;
    public static final int MAX_ACTION_LENGTH = 220;
    public static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-z_]+)}}");
    public static final String SUBJECT = "assunto";
    public static final String CATEGORY = "categoria";
    public static final String CURRENT_MONTH = "mes_atual";
    public static final String COMPARISON_MONTH = "mes_comparacao";

    public InsightContract {
        if (maxInsights < 1 || maxInsights > MAX_INSIGHTS) {
            throw new IllegalArgumentException("maxInsights must be between 1 and " + MAX_INSIGHTS);
        }
    }

    public static Set<String> placeholders(InsightCandidate candidate) {
        Set<String> names = new LinkedHashSet<>();
        if (candidate.subject() != null) {
            names.add(SUBJECT);
        }
        if (candidate.categoryName() != null) {
            names.add(CATEGORY);
        }
        names.add(CURRENT_MONTH);
        if (candidate.comparisonBucketKey() != null) {
            names.add(COMPARISON_MONTH);
        }
        candidate.metrics().forEach(metric -> names.add(metric.name()));
        return names;
    }
}
